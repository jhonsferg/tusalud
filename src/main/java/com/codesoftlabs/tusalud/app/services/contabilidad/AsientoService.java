package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.AsientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.LineaAsientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResumen;
import com.codesoftlabs.tusalud.app.entities.Asiento;
import com.codesoftlabs.tusalud.app.entities.AsientoLinea;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.entities.PeriodoContable;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AsientoRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaContableRepository;
import com.codesoftlabs.tusalud.app.repositories.PeriodoContableRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsientoService {

    private static final String ENTIDAD = "ASIENTO";
    private static final int LIMITE = 300;

    public record LineaCalculada(CuentaContable cuenta, BigDecimal debe, BigDecimal haber, String glosa) {}

    private final AsientoRepository asientos;
    private final CuentaContableRepository cuentas;
    private final PeriodoContableRepository periodos;
    private final PeriodoContableService periodoService;
    private final ContabilidadSecuencias secuencias;
    private final UsuarioRepository usuarios;
    private final ContabilidadAuditor auditor;

    public List<AsientoResumen> listar(
            UUID periodoId, EstadoAsiento estado, OrigenAsiento origen, LocalDate desde, LocalDate hasta, String q) {
        String termino = q == null || q.isBlank() ? null : "%" + q.strip().toLowerCase() + "%";
        return asientos.filtrar(periodoId, estado, origen, desde, hasta, termino, PageRequest.of(0, LIMITE)).stream()
                .map(ContabilidadMapper::resumen)
                .toList();
    }

    public AsientoResponse obtener(UUID id) {
        Asiento asiento = detalle(id);
        UUID reversaId = asientos.findByRevierteAId(id).map(Asiento::getId).orElse(null);
        return ContabilidadMapper.asiento(asiento, reversaId);
    }

    @Transactional
    public AsientoResponse crear(UsuarioPrincipal actor, AsientoRequest request) {
        validarFechaNoFutura(request.fecha());
        PeriodoContable periodo = periodoService.abiertoParaManual(request.fecha());
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        Asiento asiento = Asiento.builder()
                .fecha(request.fecha())
                .periodo(periodo)
                .glosa(request.glosa().strip())
                .origen(OrigenAsiento.MANUAL)
                .estado(EstadoAsiento.BORRADOR)
                .creadoPor(usuario)
                .creadoEn(LocalDateTime.now())
                .build();
        llenar(asiento, request.lineas());
        asiento = asientos.save(asiento);
        auditor.registrar(usuario, ENTIDAD, asiento.getId(), "CREAR", asiento.getGlosa());
        return obtener(asiento.getId());
    }

    @Transactional
    public AsientoResponse actualizar(UsuarioPrincipal actor, UUID id, AsientoRequest request) {
        Asiento asiento = detalle(id);
        exigirBorrador(asiento);
        validarFechaNoFutura(request.fecha());
        PeriodoContable periodo = periodoService.abiertoParaManual(request.fecha());
        asiento.setFecha(request.fecha());
        asiento.setPeriodo(periodo);
        asiento.setGlosa(request.glosa().strip());
        asiento.getLineas().clear();
        asientos.flush();
        llenar(asiento, request.lineas());
        auditor.registrar(usuarios.getReferenceById(actor.getId()), ENTIDAD, id, "ACTUALIZAR", asiento.getGlosa());
        return obtener(id);
    }

    @Transactional
    public void eliminar(UsuarioPrincipal actor, UUID id) {
        Asiento asiento = detalle(id);
        exigirBorrador(asiento);
        if (asiento.getOrigen() != OrigenAsiento.MANUAL) {
            throw new ConflictException("Solo se eliminan borradores manuales");
        }
        auditor.registrar(usuarios.getReferenceById(actor.getId()), ENTIDAD, id, "ELIMINAR", asiento.getGlosa());
        asientos.delete(asiento);
    }

    @Transactional
    public AsientoResponse contabilizar(UsuarioPrincipal actor, UUID id) {
        Asiento asiento = detalle(id);
        exigirBorrador(asiento);
        PeriodoContable periodo = bloquearAbierto(asiento.getPeriodo().getId());
        if (asiento.getLineas().size() < 2) {
            throw new ConflictException("El asiento necesita al menos dos líneas");
        }
        asiento.recalcularTotales();
        if (!asiento.cuadra()) {
            throw new ConflictException(
                    "El asiento no cuadra: debe " + asiento.getTotalDebe() + " y haber " + asiento.getTotalHaber());
        }
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        asiento.setPeriodo(periodo);
        asiento.setNumero(numerar(asiento.getFecha()));
        asiento.setEstado(EstadoAsiento.CONTABILIZADO);
        asiento.setContabilizadoPor(usuario);
        asiento.setContabilizadoEn(LocalDateTime.now());
        auditor.registrar(usuario, ENTIDAD, id, "CONTABILIZAR", asiento.getNumero());
        return obtener(id);
    }

    @Transactional
    public AsientoResponse reversar(UsuarioPrincipal actor, UUID id, String motivo) {
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        Asiento reversa = reversarInterno(id, motivo.strip(), usuario);
        return obtener(reversa.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Asiento registrarAutomatico(
            OrigenAsiento origen, UUID referenciaId, LocalDate fecha, String glosa, List<LineaCalculada> lineas) {
        if (referenciaId != null && asientos.existsByOrigenAndReferenciaId(origen, referenciaId)) {
            return asientos.findByOrigenAndReferenciaId(origen, referenciaId).orElseThrow();
        }
        PeriodoContable periodo =
                bloquearAbierto(periodoService.abiertoParaAutomatico(fecha).getId());
        Asiento asiento = Asiento.builder()
                .fecha(fecha)
                .periodo(periodo)
                .glosa(recortar(glosa))
                .origen(origen)
                .referenciaId(referenciaId)
                .estado(EstadoAsiento.CONTABILIZADO)
                .creadoEn(LocalDateTime.now())
                .contabilizadoEn(LocalDateTime.now())
                .build();
        for (LineaCalculada linea : lineas) {
            asiento.agregarLinea(AsientoLinea.builder()
                    .cuenta(linea.cuenta())
                    .debe(linea.debe())
                    .haber(linea.haber())
                    .glosa(linea.glosa())
                    .build());
        }
        asiento.recalcularTotales();
        if (!asiento.cuadra() || asiento.getLineas().size() < 2) {
            throw new ConflictException("El asiento automático no cuadra");
        }
        asiento.setNumero(numerar(fecha));
        asiento = asientos.saveAndFlush(asiento);
        auditor.registrar(null, ENTIDAD, asiento.getId(), "AUTOMATICO", origen + " " + asiento.getNumero());
        return asiento;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reversarAutomatico(OrigenAsiento origen, UUID referenciaId, String motivo) {
        Asiento original =
                asientos.findByOrigenAndReferenciaId(origen, referenciaId).orElse(null);
        if (original == null || original.getEstado() != EstadoAsiento.CONTABILIZADO) {
            return;
        }
        reversarInterno(original.getId(), motivo, null);
    }

    private Asiento reversarInterno(UUID id, String motivo, Usuario usuario) {
        Asiento original = detalle(id);
        if (original.getEstado() != EstadoAsiento.CONTABILIZADO) {
            throw new ConflictException("Solo se reversan asientos contabilizados");
        }
        if (original.getOrigen() == OrigenAsiento.REVERSA) {
            throw new ConflictException("Un asiento de reversa no se reversa");
        }
        LocalDate hoy = LocalDate.now();
        PeriodoContable periodo =
                bloquearAbierto(periodoService.abiertoParaAutomatico(hoy).getId());
        Asiento reversa = Asiento.builder()
                .fecha(hoy)
                .periodo(periodo)
                .glosa(recortar("Reversa de " + original.getNumero() + ": " + motivo))
                .origen(OrigenAsiento.REVERSA)
                .referenciaId(original.getId())
                .estado(EstadoAsiento.CONTABILIZADO)
                .creadoPor(usuario)
                .creadoEn(LocalDateTime.now())
                .contabilizadoPor(usuario)
                .contabilizadoEn(LocalDateTime.now())
                .revierteA(original)
                .motivoReversa(motivo)
                .build();
        for (AsientoLinea linea : original.getLineas()) {
            reversa.agregarLinea(AsientoLinea.builder()
                    .cuenta(linea.getCuenta())
                    .debe(linea.getHaber())
                    .haber(linea.getDebe())
                    .glosa(linea.getGlosa())
                    .build());
        }
        reversa.recalcularTotales();
        reversa.setNumero(numerar(hoy));
        try {
            reversa = asientos.saveAndFlush(reversa);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("El asiento " + original.getNumero() + " ya fue reversado");
        }
        original.setEstado(EstadoAsiento.REVERTIDO);
        auditor.registrar(usuario, ENTIDAD, original.getId(), "REVERSAR", reversa.getNumero() + ": " + motivo);
        return reversa;
    }

    private void llenar(Asiento asiento, List<LineaAsientoRequest> lineas) {
        List<CuentaContable> resueltas = new ArrayList<>();
        for (LineaAsientoRequest linea : lineas) {
            CuentaContable cuenta = cuentas.findById(linea.cuentaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", linea.cuentaId()));
            if (!cuenta.isActiva() || !cuenta.isAceptaMovimientos()) {
                throw new ConflictException("La cuenta " + cuenta.getCodigo() + " no admite movimientos");
            }
            resueltas.add(cuenta);
        }
        for (int i = 0; i < lineas.size(); i++) {
            LineaAsientoRequest linea = lineas.get(i);
            BigDecimal debe = importe(linea.debe());
            BigDecimal haber = importe(linea.haber());
            if (debe.signum() > 0 == haber.signum() > 0) {
                throw new DatosInvalidosException(
                        "La línea " + (i + 1) + " debe tener un importe en debe o en haber, no ambos");
            }
            asiento.agregarLinea(AsientoLinea.builder()
                    .cuenta(resueltas.get(i))
                    .debe(debe)
                    .haber(haber)
                    .glosa(
                            linea.glosa() == null || linea.glosa().isBlank()
                                    ? null
                                    : linea.glosa().strip())
                    .build());
        }
        asiento.recalcularTotales();
    }

    private PeriodoContable bloquearAbierto(UUID periodoId) {
        PeriodoContable periodo = periodos.bloquear(periodoId)
                .orElseThrow(() -> new ResourceNotFoundException("Periodo", "id", periodoId));
        if (!periodo.abierto()) {
            throw new ConflictException("El periodo " + periodo.etiqueta() + " está cerrado");
        }
        return periodo;
    }

    private String numerar(LocalDate fecha) {
        int numero = secuencias.siguiente(ContabilidadSecuencias.ASIENTO, fecha.getYear());
        return ContabilidadSecuencias.formatear(ContabilidadSecuencias.ASIENTO, fecha.getYear(), numero);
    }

    private Asiento detalle(UUID id) {
        return asientos.conDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Asiento", "id", id));
    }

    private static void exigirBorrador(Asiento asiento) {
        if (asiento.getEstado() != EstadoAsiento.BORRADOR) {
            throw new ConflictException("El asiento ya está contabilizado y no admite cambios");
        }
    }

    private static void validarFechaNoFutura(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new ConflictException("La fecha del asiento no puede ser futura");
        }
    }

    private static BigDecimal importe(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String recortar(String texto) {
        return texto.length() <= 200 ? texto : texto.substring(0, 200);
    }
}
