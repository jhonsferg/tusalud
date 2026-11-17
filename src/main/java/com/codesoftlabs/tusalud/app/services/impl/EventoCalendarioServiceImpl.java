package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CompletarEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.EventoCalendarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MoverEventoRequest;
import com.codesoftlabs.tusalud.app.dto.response.EventoCalendarioResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.EventoCalendario;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvento;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.EventoCalendarioRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoCalendarioService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventoCalendarioServiceImpl implements EventoCalendarioService {

    private static final int MAX_DIAS_RANGO = 100;
    private static final long MAX_DIAS_EVENTO = 31;
    private static final long VENTANA_CITA_MINUTOS = 240;
    private static final int DURACION_CITA_POR_DEFECTO = 30;

    private static final Set<TipoEvento> TIPOS_PERSONAL =
            EnumSet.of(TipoEvento.REUNION, TipoEvento.TAREA, TipoEvento.RECORDATORIO, TipoEvento.PERSONAL);

    private static final Map<RolNombre, Set<TipoEvento>> TIPOS_POR_ROL = Map.of(
            RolNombre.ROLE_PACIENTE,
            EnumSet.of(TipoEvento.PERSONAL, TipoEvento.RECORDATORIO),
            RolNombre.ROLE_ADMIN,
            EnumSet.of(TipoEvento.REUNION, TipoEvento.TAREA, TipoEvento.RECORDATORIO, TipoEvento.PERSONAL),
            RolNombre.ROLE_MEDICO,
            EnumSet.allOf(TipoEvento.class),
            RolNombre.ROLE_LABORATORIO,
            TIPOS_PERSONAL,
            RolNombre.ROLE_FARMACIA,
            TIPOS_PERSONAL,
            RolNombre.ROLE_ALMACEN,
            TIPOS_PERSONAL,
            RolNombre.ROLE_RECEPCION,
            TIPOS_PERSONAL,
            RolNombre.ROLE_CAJA,
            TIPOS_PERSONAL,
            RolNombre.ROLE_ENFERMERIA,
            TIPOS_PERSONAL);

    private static final List<TipoEvento> ORDEN_TIPOS = List.of(
            TipoEvento.PERSONAL, TipoEvento.RECORDATORIO, TipoEvento.REUNION, TipoEvento.TAREA, TipoEvento.BLOQUEO);

    private final EventoCalendarioRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final CitaRepository citaRepository;
    private final CitaResponseMapper citaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EventoCalendarioResponse> listar(UsuarioPrincipal actor, LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new DatosInvalidosException("Debe indicar la fecha de inicio y la de fin");
        }
        if (fin.isBefore(inicio)) {
            throw new DatosInvalidosException("La fecha de fin no puede ser anterior a la de inicio");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAX_DIAS_RANGO) {
            throw new DatosInvalidosException("El rango no puede superar " + MAX_DIAS_RANGO + " días");
        }
        return eventoRepository
                .findSolapados(
                        actor.getId(), inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay())
                .stream()
                .map(EventoCalendarioResponse::from)
                .toList();
    }

    @Override
    public EventoCalendarioResponse crear(UsuarioPrincipal actor, EventoCalendarioRequest request) {
        Usuario usuario = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        boolean todoElDia = Boolean.TRUE.equals(request.todoElDia());
        LocalDateTime[] horario = normalizar(request.inicio(), request.fin(), todoElDia);
        validarTipo(actor, request.tipo(), horario[0], horario[1]);
        EventoCalendario evento = EventoCalendario.builder()
                .usuario(usuario)
                .titulo(request.titulo().trim())
                .descripcion(limpiar(request.descripcion()))
                .ubicacion(limpiar(request.ubicacion()))
                .tipo(request.tipo())
                .inicio(horario[0])
                .fin(horario[1])
                .todoElDia(todoElDia)
                .color(request.color())
                .completado(esTareaCompletada(request))
                .recordatorioMinutos(request.recordatorioMinutos())
                .build();
        return EventoCalendarioResponse.from(eventoRepository.save(evento));
    }

    @Override
    public EventoCalendarioResponse actualizar(UsuarioPrincipal actor, UUID id, EventoCalendarioRequest request) {
        EventoCalendario evento = buscar(actor, id);
        boolean todoElDia = Boolean.TRUE.equals(request.todoElDia());
        LocalDateTime[] horario = normalizar(request.inicio(), request.fin(), todoElDia);
        validarTipo(actor, request.tipo(), horario[0], horario[1]);
        boolean cambioHorario = !horario[0].equals(evento.getInicio())
                || !horario[1].equals(evento.getFin())
                || !Objects.equals(request.recordatorioMinutos(), evento.getRecordatorioMinutos());
        evento.setTitulo(request.titulo().trim());
        evento.setDescripcion(limpiar(request.descripcion()));
        evento.setUbicacion(limpiar(request.ubicacion()));
        evento.setTipo(request.tipo());
        evento.setInicio(horario[0]);
        evento.setFin(horario[1]);
        evento.setTodoElDia(todoElDia);
        evento.setColor(request.color());
        evento.setCompletado(esTareaCompletada(request));
        evento.setRecordatorioMinutos(request.recordatorioMinutos());
        if (cambioHorario) {
            evento.setRecordatorioEnviado(false);
        }
        return EventoCalendarioResponse.from(eventoRepository.save(evento));
    }

    @Override
    public EventoCalendarioResponse mover(UsuarioPrincipal actor, UUID id, MoverEventoRequest request) {
        EventoCalendario evento = buscar(actor, id);
        boolean todoElDia = request.todoElDia() != null ? request.todoElDia() : evento.isTodoElDia();
        LocalDateTime[] horario = normalizar(request.inicio(), request.fin(), todoElDia);
        if (evento.getTipo() == TipoEvento.BLOQUEO) {
            validarSinCitasActivas(actor, horario[0], horario[1]);
        }
        evento.setInicio(horario[0]);
        evento.setFin(horario[1]);
        evento.setTodoElDia(todoElDia);
        evento.setRecordatorioEnviado(false);
        return EventoCalendarioResponse.from(eventoRepository.save(evento));
    }

    @Override
    public EventoCalendarioResponse completar(UsuarioPrincipal actor, UUID id, CompletarEventoRequest request) {
        EventoCalendario evento = buscar(actor, id);
        if (evento.getTipo() != TipoEvento.TAREA) {
            throw new DatosInvalidosException("Solo las tareas se pueden marcar como completadas");
        }
        evento.setCompletado(request.completado());
        return EventoCalendarioResponse.from(eventoRepository.save(evento));
    }

    @Override
    public void eliminar(UsuarioPrincipal actor, UUID id) {
        eventoRepository.delete(buscar(actor, id));
    }

    private EventoCalendario buscar(UsuarioPrincipal actor, UUID id) {
        return eventoRepository
                .findByIdAndUsuarioId(id, actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
    }

    private static LocalDateTime[] normalizar(LocalDateTime inicio, LocalDateTime fin, boolean todoElDia) {
        LocalDateTime desde = inicio;
        LocalDateTime hasta = fin;
        if (todoElDia) {
            if (fin.toLocalDate().isBefore(inicio.toLocalDate())) {
                throw new DatosInvalidosException("La fecha de fin no puede ser anterior a la de inicio");
            }
            desde = inicio.toLocalDate().atStartOfDay();
            hasta = fin.toLocalDate().atTime(LocalTime.of(23, 59));
        } else if (!fin.isAfter(inicio)) {
            throw new DatosInvalidosException("La hora de fin debe ser posterior a la de inicio");
        }
        if (Duration.between(desde, hasta).compareTo(Duration.ofDays(MAX_DIAS_EVENTO)) > 0) {
            throw new DatosInvalidosException("Un evento no puede durar más de " + MAX_DIAS_EVENTO + " días");
        }
        return new LocalDateTime[] {desde, hasta};
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoEvento> tiposPermitidos(UsuarioPrincipal actor) {
        return ORDEN_TIPOS.stream().filter(tipo -> tipoPermitido(actor, tipo)).toList();
    }

    private boolean tipoPermitido(UsuarioPrincipal actor, TipoEvento tipo) {
        return actor.getRoles().stream()
                .map(RolNombre::valueOf)
                .anyMatch(rol -> TIPOS_POR_ROL.getOrDefault(rol, Set.of()).contains(tipo));
    }

    private void validarTipo(UsuarioPrincipal actor, TipoEvento tipo, LocalDateTime inicio, LocalDateTime fin) {
        if (!tipoPermitido(actor, tipo)) {
            throw new ForbiddenOperationException("Tu rol no permite crear eventos de tipo " + tipo.name());
        }
        if (tipo == TipoEvento.BLOQUEO) {
            validarSinCitasActivas(actor, inicio, fin);
        }
    }

    private void validarSinCitasActivas(UsuarioPrincipal actor, LocalDateTime inicio, LocalDateTime fin) {
        Medico medico = medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo los médicos pueden crear bloqueos de agenda"));
        List<Cita> candidatas =
                citaRepository.findAgendaMedico(medico.getId(), inicio.minusMinutes(VENTANA_CITA_MINUTOS), fin);
        boolean cruce = candidatas.stream()
                .filter(cita -> cita.getEstado() == EstadoCita.PENDIENTE || cita.getEstado() == EstadoCita.CONFIRMADA)
                .anyMatch(cita -> {
                    int duracion = citaMapper.duracionDe(cita);
                    int minutos = duracion > 0 ? duracion : DURACION_CITA_POR_DEFECTO;
                    return cita.getFechaHora().isBefore(fin)
                            && cita.getFechaHora().plusMinutes(minutos).isAfter(inicio);
                });
        if (cruce) {
            throw new ConflictException("Hay citas activas en ese horario");
        }
    }

    private static boolean esTareaCompletada(EventoCalendarioRequest request) {
        return request.tipo() == TipoEvento.TAREA && Boolean.TRUE.equals(request.completado());
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
