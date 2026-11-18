package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ReprogramarRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadRangoResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.EventoCalendario;
import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Pago;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.EventoCalendarioRepository;
import com.codesoftlabs.tusalud.app.repositories.HorarioAtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.TextoSeguro;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CitaServiceImpl implements CitaService {

    private static final Locale LOCALE_ES = Locale.of("es", "PE");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int DIAS_RANGO_POR_DEFECTO = 30;
    private static final UUID NINGUNA_CITA = new UUID(0L, 0L);

    private static final Set<EstadoCita> ESTADOS_CANCELABLES = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    private static final Set<EstadoCita> ESTADOS_ATENDIBLES = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private static final int MAX_DIAS_CALENDARIO = 62;
    private static final long VENTANA_MAXIMA_MINUTOS = 240;

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final HorarioAtencionRepository horarioRepository;
    private final EventoCalendarioRepository eventoRepository;
    private final NotificacionService notificacionService;
    private final CitaResponseMapper citaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CitaResponse crearCita(CitaCreateRequest request, UsuarioPrincipal actor) {
        Paciente paciente = pacienteRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "email", actor.getEmail()));

        Medico medico = medicoRepository
                .findByIdForUpdate(request.medicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", request.medicoId()));
        if (!medico.getUsuario().isActivo()) {
            throw new BusinessLogicException("El médico seleccionado no se encuentra activo");
        }

        Especialidad especialidad = medico.getEspecialidades().stream()
                .filter(e -> e.getId().equals(request.especialidadId()))
                .findFirst()
                .orElseThrow(
                        () -> new BusinessLogicException("El médico seleccionado no atiende la especialidad indicada"));

        LocalDateTime fechaHora = request.fechaHora().truncatedTo(ChronoUnit.MINUTES);
        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw new BusinessLogicException("La fecha y hora de la cita debe ser futura");
        }

        validarDisponibilidad(medico, paciente, fechaHora, NINGUNA_CITA);

        Cita cita = Cita.builder()
                .fechaHora(fechaHora)
                .motivo(request.motivo().trim())
                .estado(EstadoCita.PENDIENTE)
                .monto(especialidad.getPrecioConsulta())
                .especialidad(especialidad)
                .medico(medico)
                .paciente(paciente)
                .build();
        cita = citaRepository.save(cita);

        String fecha = fechaHora.format(FORMATO_FECHA);
        notificacionService.crear(
                medico.getUsuario(),
                TipoNotificacion.CITA_CREADA,
                "Nueva cita registrada",
                paciente.getUsuario().getNombreCompleto() + " registró una cita para el " + fecha + ".",
                enlace(cita));
        notificacionService.crear(
                paciente.getUsuario(),
                TipoNotificacion.CITA_CREADA,
                "Cita registrada",
                "Su cita con " + medico.getUsuario().getNombreCompleto() + " para el " + fecha
                        + " está pendiente de confirmación.",
                enlace(cita));

        log.info(
                "Cita {} registrada: paciente {} con médico {} el {}",
                cita.getId(),
                paciente.getId(),
                medico.getId(),
                fechaHora);
        return citaMapper.mapear(cita);
    }

    @Override
    public List<CitaResponse> listarMisCitas(UsuarioPrincipal actor, LocalDate inicio, LocalDate fin) {
        Paciente paciente = pacienteRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "email", actor.getEmail()));

        LocalDate hoy = LocalDate.now();
        LocalDate desde = inicio != null ? inicio : hoy;
        LocalDate hasta = fin != null ? fin : hoy.plusDays(DIAS_RANGO_POR_DEFECTO);
        validarRango(desde, hasta);

        return citaRepository
                .findByPacienteAndRango(paciente.getId(), desde.atStartOfDay(), hasta.atTime(LocalTime.MAX))
                .stream()
                .map(CitaResponse::from)
                .toList();
    }

    @Override
    public CitaResponse obtenerCita(UUID citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        if (!esAdmin(actor) && !esPacienteDe(cita, actor) && !esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("No tiene permiso para ver esta cita");
        }
        return citaMapper.mapear(cita);
    }

    @Override
    @Transactional
    public CitaResponse cancelarCita(UUID citaId, String motivo, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        boolean admin = esAdmin(actor);
        if (!admin && !esPacienteDe(cita, actor) && !esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException(
                    "Solo el paciente, el médico de la cita o un administrador pueden cancelarla");
        }
        String motivoLimpio = TextoSeguro.limpiar(motivo);
        if (motivoLimpio == null || motivoLimpio.length() < 5 || motivoLimpio.length() > 300) {
            throw new DatosInvalidosException("El motivo de la cancelación debe tener entre 5 y 300 caracteres");
        }
        if (!ESTADOS_CANCELABLES.contains(cita.getEstado())) {
            throw new BusinessLogicException("No se puede cancelar una cita en estado " + cita.getEstado());
        }
        if (!admin && !cita.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new BusinessLogicException("No se puede cancelar una cita cuya fecha y hora ya pasó");
        }
        Pago pago = cita.getPago();
        if (pago != null && pago.getEstado() == EstadoPago.PAGADO) {
            pago.setEstado(EstadoPago.REEMBOLSADO);
            notificacionService.crear(
                    cita.getPaciente().getUsuario(),
                    TipoNotificacion.PAGO_REEMBOLSADO,
                    "Pago reembolsado",
                    "Se reembolsó el pago " + pago.getComprobante() + " de su cita cancelada del "
                            + cita.getFechaHora().format(FORMATO_FECHA) + ".",
                    enlace(cita));
        }
        cita.setMotivoCancelacion(motivoLimpio);
        cita.setCanceladaPor(usuarioRepository.getReferenceById(actor.getId()));
        CitaResponse cancelada = cambiarEstado(cita, EstadoCita.CANCELADA, actor);
        log.info("Auditoría: cita {} cancelada por {} con motivo registrado", cita.getId(), actor.getId());
        notificarOtraParte(
                cita,
                actor,
                TipoNotificacion.CITA_CANCELADA,
                "Cita cancelada",
                "La cita del " + cita.getFechaHora().format(FORMATO_FECHA) + " fue cancelada. Motivo: " + motivoLimpio);
        return cancelada;
    }

    @Override
    @Transactional
    public CitaResponse confirmarCita(UUID citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        if (!esAdmin(actor) && !esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("Solo el médico de la cita o un administrador pueden confirmarla");
        }
        if (cita.getEstado() != EstadoCita.PENDIENTE) {
            throw new BusinessLogicException("No se puede confirmar una cita en estado " + cita.getEstado());
        }
        CitaResponse confirmada = cambiarEstado(cita, EstadoCita.CONFIRMADA, actor);
        notificacionService.crear(
                cita.getPaciente().getUsuario(),
                TipoNotificacion.CITA_CONFIRMADA,
                "Cita confirmada",
                cita.getMedico().getUsuario().getNombreCompleto() + " confirmó su cita del "
                        + cita.getFechaHora().format(FORMATO_FECHA) + ".",
                enlace(cita));
        return confirmada;
    }

    @Override
    @Transactional
    public CitaResponse atenderCita(UUID citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        if (!esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("Solo el médico de la cita puede marcarla como atendida");
        }
        if (!ESTADOS_ATENDIBLES.contains(cita.getEstado())) {
            throw new BusinessLogicException("No se puede atender una cita en estado " + cita.getEstado());
        }
        CitaResponse atendida = cambiarEstado(cita, EstadoCita.ATENDIDA, actor);
        notificacionService.crear(
                cita.getPaciente().getUsuario(),
                TipoNotificacion.CITA_ATENDIDA,
                "Cita atendida",
                "Su cita del " + cita.getFechaHora().format(FORMATO_FECHA) + " con "
                        + cita.getMedico().getUsuario().getNombreCompleto() + " fue marcada como atendida.",
                enlace(cita));
        return atendida;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CitaResponse reprogramarCita(UUID citaId, ReprogramarRequest request, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        if (!esAdmin(actor) && !esPacienteDe(cita, actor)) {
            throw new ForbiddenOperationException(
                    "Solo el paciente de la cita o un administrador pueden reprogramarla");
        }
        if (!ESTADOS_CANCELABLES.contains(cita.getEstado())) {
            throw new BusinessLogicException("No se puede reprogramar una cita en estado " + cita.getEstado());
        }
        LocalDateTime nuevaFecha = request.fechaHora().truncatedTo(ChronoUnit.MINUTES);
        if (!nuevaFecha.isAfter(LocalDateTime.now())) {
            throw new BusinessLogicException("La fecha y hora de la cita debe ser futura");
        }
        if (nuevaFecha.equals(cita.getFechaHora())) {
            throw new BusinessLogicException("La nueva fecha y hora debe ser distinta de la actual");
        }

        Medico medico = medicoRepository
                .findByIdForUpdate(cita.getMedico().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Médico", "id", cita.getMedico().getId()));
        validarDisponibilidad(medico, cita.getPaciente(), nuevaFecha, cita.getId());

        String anterior = cita.getFechaHora().format(FORMATO_FECHA);
        cita.setFechaHora(nuevaFecha);
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setRecordatorioEnviado(false);
        Cita guardada = citaRepository.saveAndFlush(cita);
        log.info("Cita {} reprogramada de {} a {} por {}", cita.getId(), anterior, nuevaFecha, actor.getId());

        notificarOtraParte(
                guardada,
                actor,
                TipoNotificacion.CITA_REPROGRAMADA,
                "Cita reprogramada",
                "La cita del " + anterior + " fue reprogramada para el " + nuevaFecha.format(FORMATO_FECHA) + ".");
        return citaMapper.mapear(guardada);
    }

    @Override
    public List<CitaResponse> agendaMedico(UsuarioPrincipal actor, LocalDate fecha) {
        Medico medico = medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "email", actor.getEmail()));
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        return citaMapper.mapear(citaRepository.findAgendaMedicoPorFecha(medico.getId(), dia));
    }

    @Override
    public List<CitaResponse> agendaMedico(UsuarioPrincipal actor, LocalDate fecha, LocalDate inicio, LocalDate fin) {
        if (inicio == null && fin == null) {
            return agendaMedico(actor, fecha);
        }
        validarRangoCalendario(inicio, fin);
        Medico medico = medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "email", actor.getEmail()));
        return citaMapper.mapear(citaRepository.findAgendaMedico(
                medico.getId(), inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()));
    }

    @Override
    public DisponibilidadResponse consultarDisponibilidad(UUID medicoId, LocalDate fecha) {
        Medico medico = medicoRepository
                .findByIdWithDetails(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", medicoId));

        LocalDate hoy = LocalDate.now();
        LocalDate dia = fecha != null ? fecha : hoy;
        if (dia.isBefore(hoy)) {
            throw new BusinessLogicException("No se puede consultar disponibilidad de una fecha pasada");
        }

        List<HorarioAtencion> horarios = horarioRepository.findByMedicoAndDia(medicoId, dia.getDayOfWeek());
        List<LocalTime> ocupadas =
                citaRepository
                        .findHorasOcupadas(
                                medicoId, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay())
                        .stream()
                        .map(LocalDateTime::toLocalTime)
                        .toList();
        LocalTime ahora = dia.equals(hoy) ? LocalTime.now() : null;
        List<EventoCalendario> bloqueos = eventoRepository.findBloqueosSolapados(
                medico.getUsuario().getId(), dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());

        return new DisponibilidadResponse(
                medico.getId(),
                medico.getUsuario().getNombreCompleto(),
                dia,
                new ArrayList<>(calcularLibres(horarios, ocupadas, ahora, dia, bloqueos)));
    }

    @Override
    public List<DisponibilidadRangoResponse> consultarDisponibilidadRango(
            UUID medicoId, LocalDate inicio, LocalDate fin) {
        validarRangoCalendario(inicio, fin);
        Medico medico = medicoRepository
                .findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", medicoId));
        LocalDate hoy = LocalDate.now();
        LocalDate primero = inicio.isBefore(hoy) ? hoy : inicio;
        if (primero.isAfter(fin)) {
            return List.of();
        }

        Map<DayOfWeek, List<HorarioAtencion>> horariosPorDia = horarioRepository.findByMedicoId(medicoId).stream()
                .collect(Collectors.groupingBy(HorarioAtencion::getDiaSemana));
        Map<LocalDate, List<LocalTime>> ocupadasPorDia =
                citaRepository
                        .findHorasOcupadas(
                                medicoId,
                                primero.atStartOfDay(),
                                fin.plusDays(1).atStartOfDay())
                        .stream()
                        .collect(Collectors.groupingBy(
                                LocalDateTime::toLocalDate,
                                Collectors.mapping(LocalDateTime::toLocalTime, Collectors.toList())));

        List<EventoCalendario> bloqueos = eventoRepository.findBloqueosSolapados(
                medico.getUsuario().getId(),
                primero.atStartOfDay(),
                fin.plusDays(1).atStartOfDay());

        List<DisponibilidadRangoResponse> dias = new ArrayList<>();
        for (LocalDate dia = primero; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            List<HorarioAtencion> bloques = horariosPorDia.get(dia.getDayOfWeek());
            if (bloques == null) {
                continue;
            }
            List<LocalTime> libres = new ArrayList<>(calcularLibres(
                    bloques,
                    ocupadasPorDia.getOrDefault(dia, List.of()),
                    dia.equals(hoy) ? LocalTime.now() : null,
                    dia,
                    bloqueos));
            dias.add(new DisponibilidadRangoResponse(dia, libres, libres.size()));
        }
        return dias;
    }

    private static TreeSet<LocalTime> calcularLibres(
            List<HorarioAtencion> horarios,
            List<LocalTime> ocupadas,
            LocalTime ahora,
            LocalDate dia,
            List<EventoCalendario> bloqueos) {
        TreeSet<LocalTime> libres = new TreeSet<>();
        for (HorarioAtencion bloque : horarios) {
            int duracion = bloque.getDuracionCitaMinutos();
            LocalTime turno = bloque.getHoraInicio();
            while (bloque.admiteCitaEn(turno)) {
                if ((ahora == null || turno.isAfter(ahora))
                        && !seSuperpone(turno, duracion, ocupadas)
                        && !chocaConBloqueo(dia.atTime(turno), duracion, bloqueos)) {
                    libres.add(turno);
                }
                LocalTime siguiente = turno.plusMinutes(duracion);
                if (!siguiente.isAfter(turno)) {
                    break;
                }
                turno = siguiente;
            }
        }
        return libres;
    }

    private static boolean chocaConBloqueo(LocalDateTime inicio, int duracion, List<EventoCalendario> bloqueos) {
        LocalDateTime fin = inicio.plusMinutes(duracion);
        return bloqueos.stream()
                .anyMatch(b -> b.getInicio().isBefore(fin) && b.getFin().isAfter(inicio));
    }

    private static void validarRangoCalendario(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new DatosInvalidosException("Debe indicar la fecha de inicio y la de fin");
        }
        if (fin.isBefore(inicio)) {
            throw new DatosInvalidosException("La fecha de fin no puede ser anterior a la de inicio");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAX_DIAS_CALENDARIO) {
            throw new DatosInvalidosException("El rango no puede superar " + MAX_DIAS_CALENDARIO + " días");
        }
    }

    private void validarDisponibilidad(Medico medico, Paciente paciente, LocalDateTime fechaHora, UUID citaExcluida) {
        DayOfWeek dia = fechaHora.getDayOfWeek();
        List<HorarioAtencion> horarios = horarioRepository.findByMedicoAndDia(medico.getId(), dia);
        if (horarios.isEmpty()) {
            throw new BusinessLogicException("El médico no tiene horario de atención el día " + nombreDia(dia));
        }
        LocalTime hora = fechaHora.toLocalTime();
        HorarioAtencion bloque = horarios.stream()
                .filter(h -> h.admiteCitaEn(hora))
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException(String.format(
                        "La hora %s no corresponde a un turno válido del médico el día %s. Bloques de atención: %s",
                        hora.format(FORMATO_HORA), nombreDia(dia), describirBloques(horarios))));

        int duracion = bloque.getDuracionCitaMinutos();
        if (chocaConBloqueo(
                fechaHora,
                duracion,
                eventoRepository.findBloqueosSolapados(
                        medico.getUsuario().getId(), fechaHora, fechaHora.plusMinutes(duracion)))) {
            throw new ConflictException("El médico no atiende en ese horario");
        }
        LocalDateTime desde = fechaHora.minusMinutes(duracion);
        LocalDateTime hasta = fechaHora.plusMinutes(duracion);

        if (citaRepository.existsSolapamiento(medico.getId(), fechaHora, citaExcluida)
                || citaRepository.existsCitaMedicoEnFranja(medico.getId(), desde, hasta, citaExcluida)) {
            throw new ConflictException("El médico ya tiene una cita registrada en ese horario");
        }
        LocalDateTime fin = fechaHora.plusMinutes(duracion);
        boolean cruce = citaRepository
                .findActivasPacienteEnVentana(
                        paciente.getId(), fechaHora.minusMinutes(VENTANA_MAXIMA_MINUTOS), fin, citaExcluida)
                .stream()
                .anyMatch(otra -> otra.getFechaHora().isBefore(fin)
                        && otra.getFechaHora()
                                .plusMinutes(citaMapper.duracionDe(otra))
                                .isAfter(fechaHora));
        if (cruce) {
            throw new ConflictException("Ya tienes otra cita activa que se cruza con ese horario");
        }
    }

    private Cita buscarCita(UUID citaId) {
        return citaRepository
                .findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
    }

    private CitaResponse cambiarEstado(Cita cita, EstadoCita nuevoEstado, UsuarioPrincipal actor) {
        EstadoCita anterior = cita.getEstado();
        cita.setEstado(nuevoEstado);
        Cita actualizada = citaRepository.saveAndFlush(cita);
        log.info("Cita {}: {} -> {} por {}", cita.getId(), anterior, nuevoEstado, actor.getId());
        return citaMapper.mapear(actualizada);
    }

    private void notificarOtraParte(
            Cita cita, UsuarioPrincipal actor, TipoNotificacion tipo, String titulo, String mensaje) {
        if (!esMedicoDe(cita, actor)) {
            notificacionService.crear(cita.getMedico().getUsuario(), tipo, titulo, mensaje, enlace(cita));
        }
        if (!esPacienteDe(cita, actor)) {
            notificacionService.crear(cita.getPaciente().getUsuario(), tipo, titulo, mensaje, enlace(cita));
        }
    }

    private static String enlace(Cita cita) {
        return "/citas/" + cita.getId();
    }

    private static boolean seSuperpone(LocalTime turno, int duracion, List<LocalTime> ocupadas) {
        return ocupadas.stream().anyMatch(ocupada -> Math.abs(ChronoUnit.MINUTES.between(turno, ocupada)) < duracion);
    }

    private static boolean esAdmin(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
    }

    private static boolean esPacienteDe(Cita cita, UsuarioPrincipal actor) {
        return cita.getPaciente().getUsuario().getId().equals(actor.getId());
    }

    private static boolean esMedicoDe(Cita cita, UsuarioPrincipal actor) {
        return cita.getMedico().getUsuario().getId().equals(actor.getId());
    }

    private static void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new BusinessLogicException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }

    private static String nombreDia(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, LOCALE_ES);
    }

    private static String describirBloques(List<HorarioAtencion> horarios) {
        return horarios.stream()
                .map(h -> String.format(
                        "%s-%s (citas de %d min)",
                        h.getHoraInicio().format(FORMATO_HORA),
                        h.getHoraFin().format(FORMATO_HORA),
                        h.getDuracionCitaMinutos()))
                .collect(Collectors.joining(", "));
    }
}
