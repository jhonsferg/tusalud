package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.HorarioAtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
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
import java.util.Set;
import java.util.TreeSet;
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
    private static final int DIAS_RANGO_POR_DEFECTO = 30;

    private static final Set<EstadoCita> ESTADOS_CANCELABLES = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    private static final Set<EstadoCita> ESTADOS_ATENDIBLES = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private final CitaRepository citaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final HorarioAtencionRepository horarioRepository;

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

        LocalDateTime fechaHora = request.fechaHora().truncatedTo(ChronoUnit.MINUTES);
        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw new BusinessLogicException("La fecha y hora de la cita debe ser futura");
        }

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
        LocalDateTime desde = fechaHora.minusMinutes(duracion);
        LocalDateTime hasta = fechaHora.plusMinutes(duracion);

        if (citaRepository.existsSolapamiento(medico.getId(), fechaHora)
                || citaRepository.existsCitaMedicoEnFranja(medico.getId(), desde, hasta)) {
            throw new ConflictException("El médico ya tiene una cita registrada en ese horario");
        }
        if (citaRepository.existsCitaPacienteEnFranja(paciente.getId(), desde, hasta)) {
            throw new ConflictException("Ya tiene otra cita registrada que se cruza con ese horario");
        }

        Cita cita = Cita.builder()
                .fechaHora(fechaHora)
                .motivo(request.motivo().trim())
                .estado(EstadoCita.PENDIENTE)
                .monto(medico.getPrecioConsulta())
                .medico(medico)
                .paciente(paciente)
                .build();
        cita = citaRepository.save(cita);

        log.info(
                "Cita {} registrada: paciente {} con médico {} el {}",
                cita.getId(),
                paciente.getId(),
                medico.getId(),
                fechaHora);
        return CitaResponse.from(cita);
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
    public CitaResponse obtenerCita(Long citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        if (!esAdmin(actor) && !esPacienteDe(cita, actor) && !esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("No tiene permiso para ver esta cita");
        }
        return CitaResponse.from(cita);
    }

    @Override
    @Transactional
    public CitaResponse cancelarCita(Long citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        boolean admin = esAdmin(actor);
        if (!admin && !esPacienteDe(cita, actor)) {
            throw new ForbiddenOperationException("Solo el paciente de la cita o un administrador pueden cancelarla");
        }
        if (!ESTADOS_CANCELABLES.contains(cita.getEstado())) {
            throw new BusinessLogicException("No se puede cancelar una cita en estado " + cita.getEstado());
        }
        if (!admin && !cita.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new BusinessLogicException("No se puede cancelar una cita cuya fecha y hora ya pasó");
        }
        return cambiarEstado(cita, EstadoCita.CANCELADA, actor);
    }

    @Override
    @Transactional
    public CitaResponse confirmarCita(Long citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        if (!esAdmin(actor) && !esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("Solo el médico de la cita o un administrador pueden confirmarla");
        }
        if (cita.getEstado() != EstadoCita.PENDIENTE) {
            throw new BusinessLogicException("No se puede confirmar una cita en estado " + cita.getEstado());
        }
        return cambiarEstado(cita, EstadoCita.CONFIRMADA, actor);
    }

    @Override
    @Transactional
    public CitaResponse atenderCita(Long citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        if (!esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("Solo el médico de la cita puede marcarla como atendida");
        }
        if (!ESTADOS_ATENDIBLES.contains(cita.getEstado())) {
            throw new BusinessLogicException("No se puede atender una cita en estado " + cita.getEstado());
        }
        return cambiarEstado(cita, EstadoCita.ATENDIDA, actor);
    }

    @Override
    public List<CitaResponse> agendaMedico(UsuarioPrincipal actor, LocalDate fecha) {
        Medico medico = medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "email", actor.getEmail()));
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        return citaRepository.findAgendaMedicoPorFecha(medico.getId(), dia).stream()
                .map(CitaResponse::from)
                .toList();
    }

    @Override
    public DisponibilidadResponse consultarDisponibilidad(Long medicoId, LocalDate fecha) {
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

        TreeSet<LocalTime> libres = new TreeSet<>();
        for (HorarioAtencion bloque : horarios) {
            int duracion = bloque.getDuracionCitaMinutos();
            LocalTime turno = bloque.getHoraInicio();
            while (bloque.admiteCitaEn(turno)) {
                if ((ahora == null || turno.isAfter(ahora)) && !seSuperpone(turno, duracion, ocupadas)) {
                    libres.add(turno);
                }
                LocalTime siguiente = turno.plusMinutes(duracion);
                if (!siguiente.isAfter(turno)) {
                    break;
                }
                turno = siguiente;
            }
        }

        return new DisponibilidadResponse(
                medico.getId(), medico.getUsuario().getNombreCompleto(), dia, new ArrayList<>(libres));
    }

    private Cita buscarCita(Long citaId) {
        return citaRepository
                .findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
    }

    private CitaResponse cambiarEstado(Cita cita, EstadoCita nuevoEstado, UsuarioPrincipal actor) {
        EstadoCita anterior = cita.getEstado();
        cita.setEstado(nuevoEstado);
        Cita actualizada = citaRepository.saveAndFlush(cita);
        log.info("Cita {}: {} -> {} por {}", cita.getId(), anterior, nuevoEstado, actor.getEmail());
        return CitaResponse.from(actualizada);
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
