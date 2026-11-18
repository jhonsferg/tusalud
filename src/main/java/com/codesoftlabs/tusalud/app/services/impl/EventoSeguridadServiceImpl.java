package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.EventoSeguridadResponse;
import com.codesoftlabs.tusalud.app.dto.response.PaginaEventosSeguridadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenSeguridadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenSeguridadResponse.ConteoTipo;
import com.codesoftlabs.tusalud.app.dto.response.ResumenSeguridadResponse.VentanaSeguridad;
import com.codesoftlabs.tusalud.app.entities.EventoSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.SeveridadSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.repositories.EventoSeguridadRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.DispositivoParser;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
public class EventoSeguridadServiceImpl implements EventoSeguridadService {

    private static final Pattern UUID_EN_RUTA =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Duration VENTANA_DENEGADOS = Duration.ofMinutes(10);
    private static final Duration VENTANA_LOGIN = Duration.ofMinutes(15);
    private static final Duration VENTANA_DESCARGAS = Duration.ofMinutes(5);
    private static final int MAX_TAMANO = 100;
    private static final int MAX_DETALLE = 300;

    private final EventoSeguridadRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final int umbralDenegados;
    private final int umbralLogin;
    private final int umbralDescargas;
    private final int retencionDias;
    private final Map<UUID, Ventana> descargas = new ConcurrentHashMap<>();

    public EventoSeguridadServiceImpl(
            EventoSeguridadRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            @Value("${app.seguridad.accesos-denegados-umbral:5}") int umbralDenegados,
            @Value("${app.seguridad.login-fallidos-umbral:5}") int umbralLogin,
            @Value("${app.seguridad.descargas-umbral:30}") int umbralDescargas,
            @Value("${app.seguridad.retencion-dias:90}") int retencionDias) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.umbralDenegados = umbralDenegados;
        this.umbralLogin = umbralLogin;
        this.umbralDescargas = umbralDescargas;
        this.retencionDias = retencionDias;
    }

    @Override
    public void registrar(TipoEventoSeguridad tipo, UUID usuarioId, UUID actorId, String detalle) {
        registrar(tipo, usuarioId, actorId, detalle, contextoActual());
    }

    @Override
    @Transactional
    public void registrar(
            TipoEventoSeguridad tipo, UUID usuarioId, UUID actorId, String detalle, ContextoConexion contexto) {
        try {
            eventoRepository.save(EventoSeguridad.builder()
                    .tipo(tipo)
                    .severidad(tipo.getSeveridad())
                    .usuario(usuarioId == null ? null : usuarioRepository.getReferenceById(usuarioId))
                    .actor(actorId == null ? null : usuarioRepository.getReferenceById(actorId))
                    .ipEnmascarada(contexto == null ? null : enmascarar(contexto.ip()))
                    .dispositivo(
                            contexto == null || contexto.userAgent() == null
                                    ? null
                                    : recortar(DispositivoParser.describir(contexto.userAgent()), 120))
                    .detalle(detalle == null ? null : recortar(detalle, MAX_DETALLE))
                    .creadoEn(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn(
                    "No se pudo registrar el evento de seguridad {}: {}",
                    tipo,
                    ex.getClass().getSimpleName());
        }
    }

    @Override
    public void registrarLoginFallido(UUID usuarioId, String detalle, ContextoConexion contexto) {
        registrar(TipoEventoSeguridad.LOGIN_FALLIDO, usuarioId, null, detalle, contexto);
        if (usuarioId == null) {
            return;
        }
        long recientes = eventoRepository.countByTipoAndUsuarioIdAndCreadoEnAfter(
                TipoEventoSeguridad.LOGIN_FALLIDO,
                usuarioId,
                LocalDateTime.now().minus(VENTANA_LOGIN));
        if (recientes == umbralLogin) {
            registrar(
                    TipoEventoSeguridad.LOGIN_FALLIDOS_REPETIDOS,
                    usuarioId,
                    null,
                    recientes + " intentos fallidos en 15 minutos",
                    contexto);
        }
    }

    @Override
    public void registrarAccesoDenegado(String metodo, String ruta) {
        UUID usuarioId = usuarioActual();
        String objetivo =
                metodo + " " + (ruta == null ? "" : UUID_EN_RUTA.matcher(ruta).replaceAll("{id}"));
        ContextoConexion contexto = contextoActual();
        registrar(TipoEventoSeguridad.ACCESO_DENEGADO, usuarioId, null, objetivo, contexto);
        if (usuarioId == null) {
            return;
        }
        long recientes = eventoRepository.countByTipoAndUsuarioIdAndCreadoEnAfter(
                TipoEventoSeguridad.ACCESO_DENEGADO,
                usuarioId,
                LocalDateTime.now().minus(VENTANA_DENEGADOS));
        if (recientes == umbralDenegados) {
            registrar(
                    TipoEventoSeguridad.ACCESO_DENEGADO_REPETIDO,
                    usuarioId,
                    null,
                    recientes + " accesos denegados en 10 minutos",
                    contexto);
        }
    }

    @Override
    public void registrarDescarga(UUID usuarioId) {
        if (usuarioId == null) {
            return;
        }
        long ahora = System.currentTimeMillis();
        Ventana ventana = descargas.compute(usuarioId, (clave, actual) -> {
            if (actual == null || ahora - actual.inicio > VENTANA_DESCARGAS.toMillis()) {
                return new Ventana(ahora, 1);
            }
            return new Ventana(actual.inicio, actual.cantidad + 1);
        });
        if (ventana.cantidad == umbralDescargas) {
            registrar(
                    TipoEventoSeguridad.DESCARGA_MASIVA, usuarioId, null, ventana.cantidad + " descargas en 5 minutos");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaEventosSeguridadResponse listar(
            TipoEventoSeguridad tipo,
            SeveridadSeguridad severidad,
            LocalDate desde,
            LocalDate hasta,
            UUID usuarioId,
            int pagina,
            int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > MAX_TAMANO) {
            throw new DatosInvalidosException("La página debe ser 0 o mayor y el tamaño estar entre 1 y " + MAX_TAMANO);
        }
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new DatosInvalidosException("La fecha final no puede ser anterior a la inicial");
        }
        Specification<EventoSeguridad> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (tipo != null) {
                condiciones.add(cb.equal(raiz.get("tipo"), tipo));
            }
            if (severidad != null) {
                condiciones.add(cb.equal(raiz.get("severidad"), severidad));
            }
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("creadoEn"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                condiciones.add(
                        cb.lessThan(raiz.get("creadoEn"), hasta.plusDays(1).atStartOfDay()));
            }
            if (usuarioId != null) {
                condiciones.add(cb.equal(raiz.get("usuario").get("id"), usuarioId));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        var resultado = eventoRepository.findAll(
                filtro, PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "creadoEn")));
        List<EventoSeguridadResponse> contenido = resultado.getContent().stream()
                .map(EventoSeguridadResponse::from)
                .toList();
        return new PaginaEventosSeguridadResponse(
                contenido, pagina, tamano, resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenSeguridadResponse resumen() {
        LocalDateTime ahora = LocalDateTime.now();
        List<EventoSeguridad> inusuales = eventoRepository.recientesDeTipos(
                EnumSet.of(
                        TipoEventoSeguridad.DISPOSITIVO_NUEVO,
                        TipoEventoSeguridad.LOGIN_FALLIDOS_REPETIDOS,
                        TipoEventoSeguridad.LOGIN_CUENTA_INACTIVA,
                        TipoEventoSeguridad.BLOQUEO_INTENTOS),
                ahora.minusDays(7),
                PageRequest.of(0, 8));
        return new ResumenSeguridadResponse(
                ventana(ahora.minusHours(24)),
                ventana(ahora.minusDays(7)),
                inusuales.stream().map(EventoSeguridadResponse::from).toList());
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 6, timeUnit = java.util.concurrent.TimeUnit.HOURS, initialDelay = 10)
    public int purgar() {
        int eliminados = eventoRepository.eliminarAnteriores(LocalDateTime.now().minusDays(retencionDias));
        if (eliminados > 0) {
            log.info("Se purgaron {} eventos de seguridad anteriores a {} días", eliminados, retencionDias);
        }
        return eliminados;
    }

    private VentanaSeguridad ventana(LocalDateTime desde) {
        Map<String, ConteoTipo> acumulado = new HashMap<>();
        long total = 0;
        long altas = 0;
        long avisos = 0;
        for (Object[] fila : eventoRepository.contarDesde(desde)) {
            TipoEventoSeguridad tipo = (TipoEventoSeguridad) fila[0];
            SeveridadSeguridad severidad = (SeveridadSeguridad) fila[1];
            long cantidad = (Long) fila[2];
            acumulado.put(tipo.name(), new ConteoTipo(tipo.name(), tipo.getEtiqueta(), severidad.name(), cantidad));
            total += cantidad;
            if (severidad == SeveridadSeguridad.ALTA) {
                altas += cantidad;
            } else if (severidad == SeveridadSeguridad.AVISO) {
                avisos += cantidad;
            }
        }
        List<ConteoTipo> porTipo = acumulado.values().stream()
                .sorted(Comparator.comparingLong(ConteoTipo::cantidad).reversed())
                .toList();
        return new VentanaSeguridad(total, altas, avisos, porTipo);
    }

    private UUID usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private ContextoConexion contextoActual() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            HttpServletRequest request = servlet.getRequest();
            return ContextoConexion.desde(request);
        }
        return null;
    }

    static String enmascarar(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        if (ip.contains(":")) {
            String[] partes = ip.split(":");
            int visibles = Math.min(3, partes.length);
            return String.join(":", java.util.Arrays.copyOf(partes, visibles)) + ":xxxx";
        }
        int ultimo = ip.lastIndexOf('.');
        return ultimo < 0 ? "xxx" : ip.substring(0, ultimo) + ".x";
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }

    private record Ventana(long inicio, int cantidad) {}
}
