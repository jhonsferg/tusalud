package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ConciliarRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ImportarExtractoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ExtractoLineaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ExtractoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ResumenConciliacion;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.SugerenciaConciliacion;
import com.codesoftlabs.tusalud.app.entities.CuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.ExtractoBancario;
import com.codesoftlabs.tusalud.app.entities.ExtractoLinea;
import com.codesoftlabs.tusalud.app.entities.MovimientoTesoreria;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoLineaExtracto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CuentaTesoreriaRepository;
import com.codesoftlabs.tusalud.app.repositories.ExtractoBancarioRepository;
import com.codesoftlabs.tusalud.app.repositories.ExtractoLineaRepository;
import com.codesoftlabs.tusalud.app.repositories.MovimientoTesoreriaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConciliacionService {

    private static final String ENTIDAD = "CONCILIACION";
    private static final int MAXIMO_LINEAS = 5000;
    private static final int VENTANA_DIAS = 3;
    private static final DateTimeFormatter FECHA_LATINA = DateTimeFormatter.ofPattern("d/M/uuuu");

    private final CuentaTesoreriaRepository cuentas;
    private final MovimientoTesoreriaRepository movimientos;
    private final ExtractoBancarioRepository extractos;
    private final ExtractoLineaRepository lineas;
    private final UsuarioRepository usuarios;
    private final ContabilidadAuditor auditor;
    private final TesoreriaService tesoreria;

    @Transactional
    public ExtractoResponse importar(UsuarioPrincipal actor, ImportarExtractoRequest request) {
        CuentaTesoreria cuenta = cuenta(request.cuentaId());
        List<LineaCruda> crudas = leer(request.contenido());
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        ExtractoBancario extracto = extractos.save(ExtractoBancario.builder()
                .cuenta(cuenta)
                .archivo(request.archivo().strip())
                .lineas(0)
                .importadoPor(usuario)
                .importadoEn(LocalDateTime.now())
                .build());
        Map<String, Integer> ocurrencias = new HashMap<>();
        int nuevas = 0;
        int duplicadas = 0;
        LocalDate desde = null;
        LocalDate hasta = null;
        for (LineaCruda cruda : crudas) {
            String base = cuenta.getId() + "|" + cruda.fecha() + "|"
                    + cruda.monto().toPlainString() + "|" + cruda.descripcion() + "|" + cruda.referencia();
            int n = ocurrencias.merge(base, 1, Integer::sum);
            String huella = sha256(base + "|" + n);
            if (lineas.existsByCuentaIdAndHuella(cuenta.getId(), huella)) {
                duplicadas++;
                continue;
            }
            lineas.save(ExtractoLinea.builder()
                    .extracto(extracto)
                    .cuenta(cuenta)
                    .fecha(cruda.fecha())
                    .descripcion(cruda.descripcion())
                    .referencia(cruda.referencia())
                    .monto(cruda.monto())
                    .huella(huella)
                    .estado(EstadoLineaExtracto.PENDIENTE)
                    .build());
            nuevas++;
            desde = desde == null || cruda.fecha().isBefore(desde) ? cruda.fecha() : desde;
            hasta = hasta == null || cruda.fecha().isAfter(hasta) ? cruda.fecha() : hasta;
        }
        extracto.setLineas(nuevas);
        extracto.setDesde(desde);
        extracto.setHasta(hasta);
        auditor.registrar(
                usuario,
                ENTIDAD,
                extracto.getId(),
                "IMPORTAR",
                extracto.getArchivo() + ": " + nuevas + " nuevas, " + duplicadas + " repetidas");
        return aRespuesta(extracto, duplicadas);
    }

    public List<ExtractoResponse> extractos(UUID cuentaId) {
        return extractos.recientes(cuentaId).stream().map(e -> aRespuesta(e, 0)).toList();
    }

    public List<ExtractoLineaResponse> lineas(UUID cuentaId, EstadoLineaExtracto estado) {
        cuenta(cuentaId);
        return lineas.deCuenta(cuentaId, estado).stream()
                .map(ContabilidadMapper::lineaExtracto)
                .toList();
    }

    public List<SugerenciaConciliacion> sugerencias(UUID cuentaId) {
        cuenta(cuentaId);
        List<ExtractoLinea> pendientes = lineas.deCuenta(cuentaId, EstadoLineaExtracto.PENDIENTE);
        if (pendientes.isEmpty()) {
            return List.of();
        }
        LocalDate minimo = pendientes.stream()
                .map(ExtractoLinea::getFecha)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate maximo = pendientes.stream()
                .map(ExtractoLinea::getFecha)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        List<MovimientoTesoreria> libres =
                movimientos.sinConciliar(cuentaId, minimo.minusDays(VENTANA_DIAS), maximo.plusDays(VENTANA_DIAS));
        record Candidato(ExtractoLinea linea, MovimientoTesoreria movimiento, int puntaje) {}
        List<Candidato> candidatos = new ArrayList<>();
        for (ExtractoLinea linea : pendientes) {
            for (MovimientoTesoreria movimiento : libres) {
                if (movimiento.firmado().compareTo(linea.getMonto()) != 0) {
                    continue;
                }
                long dias =
                        Math.abs(java.time.temporal.ChronoUnit.DAYS.between(linea.getFecha(), movimiento.getFecha()));
                if (dias > VENTANA_DIAS) {
                    continue;
                }
                int puntaje = 100 - (int) dias * 10;
                if (coincideReferencia(linea, movimiento)) {
                    puntaje += 20;
                }
                candidatos.add(new Candidato(linea, movimiento, puntaje));
            }
        }
        candidatos.sort(Comparator.comparingInt(Candidato::puntaje).reversed());
        Set<UUID> lineasUsadas = new HashSet<>();
        Set<UUID> movimientosUsados = new HashSet<>();
        List<SugerenciaConciliacion> resultado = new ArrayList<>();
        for (Candidato c : candidatos) {
            if (lineasUsadas.contains(c.linea().getId())
                    || movimientosUsados.contains(c.movimiento().getId())) {
                continue;
            }
            lineasUsadas.add(c.linea().getId());
            movimientosUsados.add(c.movimiento().getId());
            resultado.add(new SugerenciaConciliacion(
                    ContabilidadMapper.lineaExtracto(c.linea()),
                    ContabilidadMapper.movimiento(c.movimiento()),
                    c.puntaje()));
        }
        return resultado;
    }

    public List<MovimientoResponse> movimientosSinConciliar(UUID cuentaId) {
        cuenta(cuentaId);
        return movimientos
                .filtrar(cuentaId, null, null, false, org.springframework.data.domain.PageRequest.of(0, 300))
                .stream()
                .map(ContabilidadMapper::movimiento)
                .toList();
    }

    @Transactional
    public ExtractoLineaResponse conciliar(UsuarioPrincipal actor, ConciliarRequest request) {
        ExtractoLinea linea = lineas.findById(request.lineaId())
                .orElseThrow(() -> new ResourceNotFoundException("Línea de extracto", "id", request.lineaId()));
        MovimientoTesoreria movimiento = movimientos
                .findById(request.movimientoId())
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento", "id", request.movimientoId()));
        if (linea.getEstado() != EstadoLineaExtracto.PENDIENTE) {
            throw new ConflictException("La línea del extracto ya fue conciliada o ignorada");
        }
        if (movimiento.isConciliado()) {
            throw new ConflictException("El movimiento ya está conciliado");
        }
        if (!linea.getCuenta().getId().equals(movimiento.getCuenta().getId())) {
            throw new ConflictException("La línea y el movimiento pertenecen a cuentas distintas");
        }
        if (movimiento.firmado().compareTo(linea.getMonto()) != 0) {
            throw new ConflictException("Los importes no coinciden");
        }
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        linea.setEstado(EstadoLineaExtracto.CONCILIADA);
        linea.setMovimiento(movimiento);
        linea.setConciliadaPor(usuario);
        linea.setConciliadaEn(LocalDateTime.now());
        movimiento.setConciliado(true);
        auditor.registrar(
                usuario, ENTIDAD, linea.getId(), "CONCILIAR", "S/ " + linea.getMonto() + " " + linea.getDescripcion());
        return ContabilidadMapper.lineaExtracto(linea);
    }

    @Transactional
    public ExtractoLineaResponse deshacer(UsuarioPrincipal actor, UUID lineaId) {
        ExtractoLinea linea = lineas.findById(lineaId)
                .orElseThrow(() -> new ResourceNotFoundException("Línea de extracto", "id", lineaId));
        if (linea.getEstado() == EstadoLineaExtracto.PENDIENTE) {
            throw new ConflictException("La línea no está conciliada ni ignorada");
        }
        if (linea.getMovimiento() != null) {
            linea.getMovimiento().setConciliado(false);
        }
        linea.setMovimiento(null);
        linea.setEstado(EstadoLineaExtracto.PENDIENTE);
        linea.setConciliadaPor(null);
        linea.setConciliadaEn(null);
        auditor.registrar(
                usuarios.getReferenceById(actor.getId()), ENTIDAD, lineaId, "DESHACER", linea.getDescripcion());
        return ContabilidadMapper.lineaExtracto(linea);
    }

    @Transactional
    public ExtractoLineaResponse ignorar(UsuarioPrincipal actor, UUID lineaId) {
        ExtractoLinea linea = lineas.findById(lineaId)
                .orElseThrow(() -> new ResourceNotFoundException("Línea de extracto", "id", lineaId));
        if (linea.getEstado() != EstadoLineaExtracto.PENDIENTE) {
            throw new ConflictException("Solo se ignoran líneas pendientes");
        }
        linea.setEstado(EstadoLineaExtracto.IGNORADA);
        linea.setConciliadaPor(usuarios.getReferenceById(actor.getId()));
        linea.setConciliadaEn(LocalDateTime.now());
        auditor.registrar(linea.getConciliadaPor(), ENTIDAD, lineaId, "IGNORAR", linea.getDescripcion());
        return ContabilidadMapper.lineaExtracto(linea);
    }

    public ResumenConciliacion resumen(UUID cuentaId) {
        CuentaTesoreria cuenta = cuenta(cuentaId);
        List<ExtractoLinea> pendientes = lineas.deCuenta(cuentaId, EstadoLineaExtracto.PENDIENTE);
        BigDecimal netoExtracto =
                pendientes.stream().map(ExtractoLinea::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MovimientoTesoreria> libres = movimientos.filtrar(
                cuentaId, null, null, false, org.springframework.data.domain.PageRequest.of(0, 5000));
        BigDecimal netoLibros =
                libres.stream().map(MovimientoTesoreria::firmado).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenConciliacion(
                cuentaId,
                tesoreria.saldo(cuenta),
                pendientes.size(),
                netoExtracto,
                libres.size(),
                netoLibros,
                netoExtracto.subtract(netoLibros));
    }

    private List<LineaCruda> leer(String contenido) {
        String[] renglones = contenido.replace("\r", "").split("\n");
        List<LineaCruda> resultado = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        char separador = detectarSeparador(renglones);
        for (int i = 0; i < renglones.length; i++) {
            String renglon = renglones[i].strip();
            if (renglon.isEmpty()) {
                continue;
            }
            String[] c = renglon.split(String.valueOf(separador == '\t' ? "\t" : "\\" + separador), -1);
            if (resultado.isEmpty() && errores.isEmpty() && esEncabezado(c)) {
                continue;
            }
            try {
                if (c.length < 3) {
                    throw new IllegalArgumentException("se esperan fecha, descripción, referencia y monto");
                }
                LocalDate fecha = fecha(c[0].strip());
                String descripcion = c[1].strip();
                if (descripcion.isEmpty()) {
                    throw new IllegalArgumentException("la descripción está vacía");
                }
                String referencia = c.length >= 4 ? c[2].strip() : "";
                BigDecimal monto = monto(c[c.length - 1].strip());
                if (monto.signum() == 0) {
                    throw new IllegalArgumentException("el monto no puede ser cero");
                }
                resultado.add(new LineaCruda(
                        fecha,
                        descripcion.length() > 200 ? descripcion.substring(0, 200) : descripcion,
                        referencia.length() > 60 ? referencia.substring(0, 60) : referencia,
                        monto));
            } catch (IllegalArgumentException | DateTimeParseException ex) {
                errores.add("línea " + (i + 1) + ": " + ex.getMessage());
            }
            if (resultado.size() > MAXIMO_LINEAS) {
                throw new DatosInvalidosException("El extracto supera el máximo de " + MAXIMO_LINEAS + " líneas");
            }
        }
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("El extracto tiene errores. "
                    + String.join("; ", errores.subList(0, Math.min(5, errores.size()))));
        }
        if (resultado.isEmpty()) {
            throw new DatosInvalidosException("El extracto no tiene movimientos");
        }
        return resultado;
    }

    private static char detectarSeparador(String[] renglones) {
        for (String renglon : renglones) {
            if (renglon.isBlank()) {
                continue;
            }
            if (renglon.contains(";")) {
                return ';';
            }
            if (renglon.contains("\t")) {
                return '\t';
            }
            return ',';
        }
        return ',';
    }

    private static boolean esEncabezado(String[] columnas) {
        return columnas[0].strip().toLowerCase().startsWith("fecha");
    }

    private static LocalDate fecha(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException ex) {
            return LocalDate.parse(texto, FECHA_LATINA);
        }
    }

    private static BigDecimal monto(String texto) {
        try {
            return new BigDecimal(texto.replace(" ", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("el monto no es válido");
        }
    }

    private static boolean coincideReferencia(ExtractoLinea linea, MovimientoTesoreria movimiento) {
        String referencia = linea.getReferencia();
        if (referencia == null || referencia.isBlank()) {
            return false;
        }
        String r = referencia.toLowerCase();
        return (movimiento.getReferencia() != null
                        && movimiento.getReferencia().toLowerCase().contains(r))
                || movimiento.getDescripcion().toLowerCase().contains(r);
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private CuentaTesoreria cuenta(UUID id) {
        return cuentas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cuenta de tesorería", "id", id));
    }

    private static ExtractoResponse aRespuesta(ExtractoBancario e, int duplicadas) {
        return new ExtractoResponse(
                e.getId(),
                e.getCuenta().getId(),
                e.getCuenta().getNombre(),
                e.getArchivo(),
                e.getDesde(),
                e.getHasta(),
                e.getLineas(),
                duplicadas,
                e.getImportadoPor().getNombreCompleto(),
                e.getImportadoEn());
    }

    private record LineaCruda(LocalDate fecha, String descripcion, String referencia, BigDecimal monto) {}
}
