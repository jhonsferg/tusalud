package com.codesoftlabs.tusalud.app.services.calidad;

import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.AccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.AnalisisRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.CausaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.CierreEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.EventoRequest;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.AccionResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.CausaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.EventoDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.EventoResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.TableroResponse;
import com.codesoftlabs.tusalud.app.entities.Cirugia;
import com.codesoftlabs.tusalud.app.entities.Dispensacion;
import com.codesoftlabs.tusalud.app.entities.EventoAccion;
import com.codesoftlabs.tusalud.app.entities.EventoAdverso;
import com.codesoftlabs.tusalud.app.entities.EventoCausa;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.EstadoAccion;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.EstadoEvento;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.EventoAdversoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventosAdversosService {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EventoAdversoRepository eventos;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final EntityManager entityManager;

    @Transactional
    public EventoResumenResponse notificar(UsuarioPrincipal actor, EventoRequest request) {
        if (request.anonimo()
                && (request.pacienteId() != null || request.cirugiaId() != null || request.dispensacionId() != null)) {
            throw new BusinessLogicException("Un reporte anónimo no puede vincular paciente, cirugía ni dispensación");
        }
        if (request.ocurridoEn().isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessLogicException("La fecha del evento no puede ser futura");
        }
        int anio = LocalDate.now().getYear();
        int numero = eventos.ultimoNumero(anio) + 1;
        EventoAdverso evento = EventoAdverso.builder()
                .anio(anio)
                .numero(numero)
                .codigo("EA-" + anio + "-" + String.format("%04d", numero))
                .tipo(request.tipo())
                .severidad(request.severidad())
                .area(request.area().strip())
                .descripcion(request.descripcion().strip())
                .anonimo(request.anonimo())
                .notificante(usuarios.getReferenceById(actor.getId()))
                .paciente(
                        request.pacienteId() == null
                                ? null
                                : entityManager.getReference(Paciente.class, request.pacienteId()))
                .cirugia(
                        request.cirugiaId() == null
                                ? null
                                : entityManager.getReference(Cirugia.class, request.cirugiaId()))
                .dispensacion(
                        request.dispensacionId() == null
                                ? null
                                : entityManager.getReference(Dispensacion.class, request.dispensacionId()))
                .ocurridoEn(request.ocurridoEn())
                .notificadoEn(LocalDateTime.now())
                .build();
        evento = eventos.save(evento);
        avisarCalidad(evento);
        return resumen(evento, true);
    }

    public List<EventoResumenResponse> mios(UsuarioPrincipal actor) {
        return eventos.delNotificante(actor.getId()).stream()
                .map(e -> resumen(e, true))
                .toList();
    }

    public List<EventoResumenResponse> listar(UsuarioPrincipal actor, EstadoEvento estado) {
        exigirGestion(actor);
        return eventos.todos().stream()
                .filter(e -> estado == null || e.getEstado() == estado)
                .map(e -> resumen(e, false))
                .toList();
    }

    public EventoDetalleResponse detalle(UsuarioPrincipal actor, UUID id) {
        EventoAdverso evento = cargar(id);
        boolean gestion = esGestion(actor);
        if (!gestion && !evento.getNotificante().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo calidad, gerencia o quien notificó ven este reporte");
        }
        return detalle(evento, gestion);
    }

    @Transactional
    public EventoDetalleResponse analizar(UsuarioPrincipal actor, UUID id, AnalisisRequest request) {
        exigirCalidad(actor);
        EventoAdverso evento = cargar(id);
        exigirAbierto(evento);
        evento.getCausas().clear();
        entityManager.flush();
        int posicion = 0;
        for (CausaRequest causa : request.causas()) {
            posicion++;
            evento.getCausas()
                    .add(EventoCausa.builder()
                            .evento(evento)
                            .posicion(posicion)
                            .categoria(causa.categoria().strip())
                            .texto(causa.texto().strip())
                            .build());
        }
        evento.setAnalisisMetodo(request.metodo());
        evento.setAnalisisConclusion(request.conclusion().strip());
        evento.setClasificacion(
                request.clasificacion() == null || request.clasificacion().isBlank()
                        ? null
                        : request.clasificacion().strip());
        if (evento.getEstado() == EstadoEvento.NOTIFICADO) {
            evento.setEstado(EstadoEvento.EN_ANALISIS);
        }
        return detalle(eventos.save(evento), true);
    }

    @Transactional
    public EventoDetalleResponse agregarAccion(UsuarioPrincipal actor, UUID id, AccionRequest request) {
        exigirCalidad(actor);
        EventoAdverso evento = cargar(id);
        exigirAbierto(evento);
        if (request.fechaLimite().isBefore(LocalDate.now())) {
            throw new BusinessLogicException("La fecha límite no puede estar en el pasado");
        }
        var responsable = usuarios.findById(request.responsableId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.responsableId()));
        evento.getAcciones()
                .add(EventoAccion.builder()
                        .evento(evento)
                        .descripcion(request.descripcion().strip())
                        .responsable(responsable)
                        .fechaLimite(request.fechaLimite())
                        .creadoEn(LocalDateTime.now())
                        .build());
        evento.setEstado(EstadoEvento.EN_ACCION);
        notificaciones.crear(
                responsable,
                TipoNotificacion.SISTEMA,
                "Acción de mejora asignada",
                "Se le asignó una acción del reporte " + evento.getCodigo() + " con fecha límite "
                        + request.fechaLimite() + ".",
                "/calidad/eventos/" + evento.getId());
        return detalle(eventos.save(evento), true);
    }

    @Transactional
    public EventoDetalleResponse completarAccion(UsuarioPrincipal actor, UUID accionId) {
        EventoAccion accion =
                eventos.accion(accionId).orElseThrow(() -> new ResourceNotFoundException("Acción", "id", accionId));
        if (!accion.getResponsable().getId().equals(actor.getId()) && !esCalidad(actor)) {
            throw new ForbiddenOperationException("Solo el responsable o calidad completan la acción");
        }
        EventoAdverso evento = cargar(accion.getEvento().getId());
        exigirAbierto(evento);
        accion.setEstado(EstadoAccion.HECHA);
        accion.setCompletadaEn(LocalDateTime.now());
        return detalle(eventos.save(evento), esGestion(actor));
    }

    @Transactional
    public EventoDetalleResponse cerrar(UsuarioPrincipal actor, UUID id, CierreEventoRequest request) {
        exigirCalidad(actor);
        EventoAdverso evento = cargar(id);
        exigirAbierto(evento);
        if (evento.getAnalisisMetodo() == null) {
            throw new BusinessLogicException("Registre el análisis de causa antes de cerrar");
        }
        if (evento.getAcciones().isEmpty()) {
            throw new BusinessLogicException("Registre al menos una acción de mejora antes de cerrar");
        }
        if (evento.getAcciones().stream().anyMatch(a -> a.getEstado() != EstadoAccion.HECHA)) {
            throw new BusinessLogicException("Hay acciones pendientes");
        }
        evento.setVerificacion(request.verificacion().strip());
        evento.setAccionEficaz(request.accionEficaz());
        evento.setCerradoEn(LocalDateTime.now());
        evento.setCerradoPor(usuarios.getReferenceById(actor.getId()));
        evento.setEstado(EstadoEvento.CERRADO);
        return detalle(eventos.save(evento), true);
    }

    public TableroResponse tablero(UsuarioPrincipal actor) {
        exigirGestion(actor);
        List<EventoAdverso> todos = eventos.todos();
        Map<String, Long> estado = contar(todos, e -> e.getEstado().name());
        Map<String, Long> severidad = contar(todos, e -> e.getSeveridad().name());
        Map<String, Long> area = contar(todos, EventoAdverso::getArea);
        Map<String, Long> mes = new TreeMap<>(contar(todos, e -> MES.format(e.getNotificadoEn())));
        long vencidas = todos.stream()
                .filter(e -> e.getEstado() != EstadoEvento.CERRADO)
                .flatMap(e -> e.getAcciones().stream())
                .filter(a -> a.getEstado() == EstadoAccion.PENDIENTE
                        && a.getFechaLimite().isBefore(LocalDate.now()))
                .count();
        double dias = todos.stream()
                        .filter(e -> e.getCerradoEn() != null)
                        .mapToLong(e -> Duration.between(e.getNotificadoEn(), e.getCerradoEn())
                                .toHours())
                        .average()
                        .orElse(0)
                / 24.0;
        long casi = todos.stream()
                .filter(e -> e.getTipo().name().equals("CASI_INCIDENTE"))
                .count();
        long adversos = todos.stream()
                .filter(e -> e.getTipo().name().equals("EVENTO_ADVERSO"))
                .count();
        return new TableroResponse(
                todos.size(), estado, severidad, area, mes, vencidas, Math.round(dias * 10.0) / 10.0, casi, adversos);
    }

    public byte[] exportar(UsuarioPrincipal actor) {
        exigirGestion(actor);
        try (XSSFWorkbook libro = new XSSFWorkbook();
                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Eventos");
            String[] columnas = {
                "Código", "Tipo", "Severidad", "Área", "Estado", "Ocurrido", "Notificado", "Acciones", "Cerrado"
            };
            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                cabecera.createCell(i).setCellValue(columnas[i]);
            }
            int fila = 1;
            for (EventoAdverso e : eventos.todos()) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(e.getCodigo());
                r.createCell(1).setCellValue(e.getTipo().name());
                r.createCell(2).setCellValue(e.getSeveridad().name());
                r.createCell(3).setCellValue(e.getArea());
                r.createCell(4).setCellValue(e.getEstado().name());
                r.createCell(5).setCellValue(e.getOcurridoEn().toString());
                r.createCell(6).setCellValue(e.getNotificadoEn().toString());
                r.createCell(7).setCellValue(e.getAcciones().size());
                r.createCell(8)
                        .setCellValue(
                                e.getCerradoEn() == null ? "" : e.getCerradoEn().toString());
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel", ex);
        }
    }

    private void avisarCalidad(EventoAdverso evento) {
        for (var usuario : usuarios.activosPorRol(RolNombre.ROLE_CALIDAD)) {
            notificaciones.crear(
                    usuario,
                    TipoNotificacion.SISTEMA,
                    "Nuevo reporte de seguridad",
                    "Se notificó el reporte " + evento.getCodigo() + " con severidad " + evento.getSeveridad() + ".",
                    "/calidad/eventos/" + evento.getId());
        }
    }

    private static Map<String, Long> contar(
            List<EventoAdverso> lista, java.util.function.Function<EventoAdverso, String> clave) {
        return lista.stream().collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.counting()));
    }

    private EventoAdverso cargar(UUID id) {
        return eventos.detalle(id).orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
    }

    private static void exigirAbierto(EventoAdverso evento) {
        if (evento.getEstado() == EstadoEvento.CERRADO) {
            throw new BusinessLogicException("El reporte ya está cerrado");
        }
    }

    private static boolean esCalidad(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_CALIDAD.name());
    }

    private static boolean esGestion(UsuarioPrincipal actor) {
        return esCalidad(actor) || actor.getRoles().contains(RolNombre.ROLE_GERENCIA.name());
    }

    private static void exigirCalidad(UsuarioPrincipal actor) {
        if (!esCalidad(actor)) {
            throw new ForbiddenOperationException("Solo calidad gestiona los reportes");
        }
    }

    private static void exigirGestion(UsuarioPrincipal actor) {
        if (!esGestion(actor)) {
            throw new ForbiddenOperationException("Solo calidad y gerencia ven todos los reportes");
        }
    }

    private static EventoResumenResponse resumen(EventoAdverso e, boolean propio) {
        String notificante =
                e.isAnonimo() && !propio ? null : e.getNotificante().getNombreCompleto();
        int pendientes = (int) e.getAcciones().stream()
                .filter(a -> a.getEstado() == EstadoAccion.PENDIENTE)
                .count();
        return new EventoResumenResponse(
                e.getId(),
                e.getCodigo(),
                e.getTipo().name(),
                e.getSeveridad().name(),
                e.getArea(),
                e.getEstado().name(),
                e.isAnonimo(),
                notificante,
                e.getOcurridoEn(),
                e.getNotificadoEn(),
                pendientes);
    }

    private static EventoDetalleResponse detalle(EventoAdverso e, boolean gestion) {
        boolean ocultar = e.isAnonimo() && gestion;
        List<CausaResponse> causas = e.getCausas().stream()
                .map(c -> new CausaResponse(c.getPosicion(), c.getCategoria(), c.getTexto()))
                .toList();
        List<AccionResponse> acciones = new ArrayList<>();
        for (EventoAccion a : e.getAcciones()) {
            acciones.add(new AccionResponse(
                    a.getId(),
                    a.getDescripcion(),
                    a.getResponsable().getId(),
                    a.getResponsable().getNombreCompleto(),
                    a.getFechaLimite(),
                    a.getEstado().name(),
                    a.getEstado() == EstadoAccion.PENDIENTE
                            && a.getFechaLimite().isBefore(LocalDate.now()),
                    a.getCompletadaEn()));
        }
        return new EventoDetalleResponse(
                e.getId(),
                e.getCodigo(),
                e.getTipo().name(),
                e.getSeveridad().name(),
                e.getArea(),
                e.getEstado().name(),
                e.isAnonimo(),
                ocultar ? null : e.getNotificante().getNombreCompleto(),
                e.getDescripcion(),
                e.getPaciente() == null ? null : e.getPaciente().getUsuario().getNombreCompleto(),
                e.getCirugia() == null ? null : e.getCirugia().getId(),
                e.getDispensacion() == null ? null : e.getDispensacion().getId(),
                e.getClasificacion(),
                e.getAnalisisMetodo() == null ? null : e.getAnalisisMetodo().name(),
                e.getAnalisisConclusion(),
                causas,
                acciones,
                e.getOcurridoEn(),
                e.getNotificadoEn(),
                e.getCerradoEn(),
                e.getCerradoPor() == null ? null : e.getCerradoPor().getNombreCompleto(),
                e.getVerificacion(),
                e.getAccionEficaz());
    }
}
