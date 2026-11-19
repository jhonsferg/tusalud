package com.codesoftlabs.tusalud.app.services.respaldos;

import com.codesoftlabs.tusalud.app.canales.FirmasCanales;
import com.codesoftlabs.tusalud.app.config.RespaldosProperties;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.RespaldoEjecucionRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.EstadoRespaldosResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.RespaldoEjecucionResponse;
import com.codesoftlabs.tusalud.app.entities.RespaldoEjecucion;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.TipoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.RespaldoEjecucionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class RespaldosService {

    private static final String ENLACE = "/respaldos";
    private static final int HORAS_ENTRE_ALERTAS = 12;

    private final RespaldoEjecucionRepository ejecuciones;
    private final UsuarioRepository usuarios;
    private final NotificacionRepository notificaciones;
    private final NotificacionService notificacionService;
    private final RespaldosProperties propiedades;
    private final LocalDateTime inicioAplicacion = LocalDateTime.now();

    public RespaldosService(
            RespaldoEjecucionRepository ejecuciones,
            UsuarioRepository usuarios,
            NotificacionRepository notificaciones,
            NotificacionService notificacionService,
            RespaldosProperties propiedades) {
        this.ejecuciones = ejecuciones;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.notificacionService = notificacionService;
        this.propiedades = propiedades;
    }

    @Transactional
    public RespaldoEjecucionResponse registrar(String tokenRecibido, RespaldoEjecucionRequest request) {
        String esperado = propiedades.token();
        if (esperado == null || esperado.isBlank() || !FirmasCanales.iguales(esperado, tokenRecibido)) {
            throw new ForbiddenOperationException("Token de respaldo inválido");
        }
        if (request.finalizadoEn().isBefore(request.iniciadoEn())) {
            throw new DatosInvalidosException("La ejecución no puede terminar antes de empezar");
        }
        RespaldoEjecucion guardada = ejecuciones.saveAndFlush(RespaldoEjecucion.builder()
                .tipo(request.tipo())
                .estado(request.estado())
                .alcance(request.alcance())
                .categoria(request.categoria())
                .iniciadoEn(request.iniciadoEn())
                .finalizadoEn(request.finalizadoEn())
                .bytesTotal(request.bytesTotal())
                .archivo(request.archivo())
                .checksum(request.checksum())
                .migracionesValidadas(request.migracionesValidadas())
                .tablasValidadas(request.tablasValidadas())
                .objetosAlmacen(request.objetosAlmacen())
                .copiaRemota(request.copiaRemota())
                .detalle(request.detalle())
                .build());
        if (request.estado() == EstadoEjecucionRespaldo.FALLIDO) {
            String que = request.tipo() == TipoEjecucionRespaldo.RESPALDO
                    ? "El respaldo programado falló"
                    : "La prueba de restauración falló";
            alertar(TipoNotificacion.RESPALDO_FALLIDO, que, que + ". Revise el detalle del estado de respaldos.");
        }
        return respuesta(guardada);
    }

    public List<RespaldoEjecucionResponse> listar(TipoEjecucionRespaldo tipo, int limite) {
        PageRequest pagina = PageRequest.of(0, Math.min(Math.max(limite, 1), 200));
        return (tipo == null
                        ? ejecuciones.findAllByOrderByIniciadoEnDesc(pagina)
                        : ejecuciones.findByTipoOrderByIniciadoEnDesc(tipo, pagina))
                .stream().map(RespaldosService::respuesta).toList();
    }

    public EstadoRespaldosResponse estado() {
        return estado(LocalDateTime.now());
    }

    public EstadoRespaldosResponse estado(LocalDateTime ahora) {
        RespaldoEjecucion exitoso = ejecuciones
                .findFirstByTipoAndEstadoOrderByFinalizadoEnDesc(
                        TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.EXITOSO)
                .orElse(null);
        RespaldoEjecucion fallido = ejecuciones
                .findFirstByTipoAndEstadoOrderByFinalizadoEnDesc(
                        TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.FALLIDO)
                .orElse(null);
        RespaldoEjecucion pruebaExitosa = ejecuciones
                .findFirstByTipoAndEstadoOrderByFinalizadoEnDesc(
                        TipoEjecucionRespaldo.PRUEBA_RESTAURACION, EstadoEjecucionRespaldo.EXITOSO)
                .orElse(null);
        RespaldoEjecucion prueba = ejecuciones
                .findFirstByTipoOrderByFinalizadoEnDesc(TipoEjecucionRespaldo.PRUEBA_RESTAURACION)
                .orElse(null);

        Long horas = exitoso == null
                ? null
                : Duration.between(exitoso.getFinalizadoEn(), ahora).toHours();
        Long dias = pruebaExitosa == null
                ? null
                : Duration.between(pruebaExitosa.getFinalizadoEn(), ahora).toDays();
        boolean respaldoVencido = horas == null
                ? Duration.between(inicioAplicacion, ahora).toHours() >= propiedades.maxHorasSinRespaldo()
                : horas >= propiedades.maxHorasSinRespaldo();
        boolean pruebaVencida = dias == null
                ? Duration.between(inicioAplicacion, ahora).toDays() >= propiedades.maxDiasSinPrueba()
                : dias >= propiedades.maxDiasSinPrueba();
        boolean ultimoFallo =
                fallido != null && (exitoso == null || fallido.getFinalizadoEn().isAfter(exitoso.getFinalizadoEn()));
        String semaforo = respaldoVencido || ultimoFallo
                ? "ROJO"
                : (pruebaVencida || exitoso == null || pruebaExitosa == null ? "AMARILLO" : "VERDE");
        return new EstadoRespaldosResponse(
                exitoso == null ? null : respuesta(exitoso),
                fallido == null ? null : respuesta(fallido),
                pruebaExitosa == null ? null : respuesta(pruebaExitosa),
                prueba == null ? null : respuesta(prueba),
                horas,
                dias,
                respaldoVencido,
                pruebaVencida,
                propiedades.maxHorasSinRespaldo(),
                propiedades.maxDiasSinPrueba(),
                propiedades.rpoObjetivoHoras(),
                propiedades.rtoObjetivoMinutos(),
                semaforo);
    }

    @Scheduled(fixedDelayString = "${app.respaldos.intervalo-vigilancia-ms:3600000}", initialDelay = 300000)
    @Transactional
    public void vigilar() {
        if (!propiedades.vigilanciaHabilitada()) {
            return;
        }
        evaluarVencimientos(LocalDateTime.now());
    }

    @Transactional
    public int evaluarVencimientos(LocalDateTime ahora) {
        EstadoRespaldosResponse estado = estado(ahora);
        int enviadas = 0;
        if (estado.respaldoVencido()) {
            enviadas += alertar(
                    TipoNotificacion.RESPALDO_VENCIDO,
                    "No hay un respaldo reciente",
                    "No hay un respaldo exitoso en las últimas " + estado.maxHorasSinRespaldo()
                            + " horas. Revise el estado de respaldos.");
        } else if (estado.pruebaVencida()) {
            enviadas += alertar(
                    TipoNotificacion.RESPALDO_VENCIDO,
                    "Falta probar la restauración",
                    "No hay una prueba de restauración exitosa en los últimos " + estado.maxDiasSinPrueba()
                            + " días. Revise el estado de respaldos.");
        }
        return enviadas;
    }

    private int alertar(TipoNotificacion tipo, String titulo, String mensaje) {
        Map<java.util.UUID, Usuario> destinatarios = new LinkedHashMap<>();
        usuarios.findActivosPorRol(RolNombre.ROLE_ADMIN).forEach(u -> destinatarios.put(u.getId(), u));
        usuarios.findActivosPorRol(RolNombre.ROLE_OPERACIONES).forEach(u -> destinatarios.put(u.getId(), u));
        LocalDateTime desde = LocalDateTime.now().minusHours(HORAS_ENTRE_ALERTAS);
        int enviadas = 0;
        for (Usuario usuario : destinatarios.values()) {
            if (notificaciones.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(usuario.getId(), tipo, desde)) {
                continue;
            }
            notificacionService.crear(usuario, tipo, titulo, mensaje, ENLACE);
            enviadas++;
        }
        log.warn("Alerta de respaldos {} enviada a {} usuarios", tipo, enviadas);
        return enviadas;
    }

    static RespaldoEjecucionResponse respuesta(RespaldoEjecucion e) {
        return new RespaldoEjecucionResponse(
                e.getId(),
                e.getTipo(),
                e.getEstado(),
                e.getAlcance(),
                e.getCategoria(),
                e.getIniciadoEn(),
                e.getFinalizadoEn(),
                Duration.between(e.getIniciadoEn(), e.getFinalizadoEn()).getSeconds(),
                e.getBytesTotal(),
                e.getArchivo(),
                e.getChecksum(),
                e.getMigracionesValidadas(),
                e.getTablasValidadas(),
                e.getObjetosAlmacen(),
                e.isCopiaRemota(),
                e.getDetalle());
    }
}
