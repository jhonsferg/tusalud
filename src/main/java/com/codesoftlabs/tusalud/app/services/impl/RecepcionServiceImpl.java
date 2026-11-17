package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.TurnoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaHoyResponse;
import com.codesoftlabs.tusalud.app.dto.response.PantallaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecepcionResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Turno;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadTurno;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.TurnoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RecepcionService;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecepcionServiceImpl implements RecepcionService {

    private static final int PANTALLA_MAXIMO = 8;
    private static final Set<EstadoCita> ESTADOS_CON_LLEGADA = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    private static final Set<EstadoTurno> ESTADOS_LLAMABLES = EnumSet.of(EstadoTurno.EN_ESPERA);
    private static final Set<EstadoTurno> ESTADOS_INICIABLES = EnumSet.of(EstadoTurno.EN_ESPERA, EstadoTurno.LLAMADO);
    private static final Set<EstadoTurno> ESTADOS_CERRABLES = EnumSet.of(EstadoTurno.EN_ESPERA, EstadoTurno.LLAMADO);

    private final TurnoRepository turnoRepository;
    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional(readOnly = true)
    public List<CitaHoyResponse> citasHoy(String q) {
        LocalDate hoy = LocalDate.now();
        List<Cita> citas =
                citaRepository
                        .findAllByRango(
                                hoy.atStartOfDay(),
                                hoy.plusDays(1).atStartOfDay().minusNanos(1))
                        .stream()
                        .filter(cita -> cita.getEstado() != EstadoCita.CANCELADA)
                        .filter(cita -> coincide(cita, q))
                        .toList();
        Map<Long, String> turnos = citas.isEmpty()
                ? Map.of()
                : turnoRepository.findByCitaIds(citas.stream().map(Cita::getId).toList()).stream()
                        .collect(Collectors.toMap(turno -> turno.getCita().getId(), Turno::getNumero));
        return citas.stream()
                .map(cita -> CitaHoyResponse.from(cita, turnos.get(cita.getId())))
                .toList();
    }

    @Override
    public TurnoResponse registrarLlegada(Long citaId) {
        Cita cita = citaRepository
                .findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
        LocalDate hoy = LocalDate.now();
        if (!ESTADOS_CON_LLEGADA.contains(cita.getEstado())) {
            throw new ConflictException("Solo las citas pendientes o confirmadas registran llegada");
        }
        if (!cita.getFechaHora().toLocalDate().equals(hoy)) {
            throw new ConflictException("La llegada solo se registra el mismo día de la cita");
        }
        if (turnoRepository.existsByCitaId(citaId)) {
            throw new ConflictException("El paciente ya registró su llegada para esta cita");
        }
        Medico medico = cita.getMedico();
        Turno turno = nuevoTurno(
                cita.getPaciente(), cita, medico, PrioridadTurno.NORMAL, cita.getMotivo(), medico.getConsultorio());
        notificacionService.crear(
                medico.getUsuario(),
                TipoNotificacion.PACIENTE_EN_ESPERA,
                "Paciente en espera",
                cita.getPaciente().getUsuario().getNombreCompleto() + " llegó para su cita y espera con el turno "
                        + turno.getNumero() + ".",
                "/cola");
        log.info("Llegada registrada: cita {} turno {}", citaId, turno.getNumero());
        return respuesta(turno);
    }

    @Override
    public TurnoResponse crearTurno(TurnoCreateRequest request) {
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        Medico medico = null;
        if (request.medicoId() != null) {
            medico = medicoRepository
                    .findByIdWithDetails(request.medicoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", request.medicoId()));
            if (!medico.getUsuario().isActivo()) {
                throw new ConflictException("El médico seleccionado no se encuentra activo");
            }
        }
        if (turnoRepository.existsAbiertoDelPaciente(paciente.getId(), LocalDate.now(), EstadoTurno.ABIERTOS)) {
            throw new ConflictException("El paciente ya tiene un turno abierto hoy");
        }
        String motivo = request.motivo() == null || request.motivo().isBlank()
                ? null
                : request.motivo().trim();
        Turno turno = nuevoTurno(
                paciente, null, medico, request.prioridad(), motivo, medico == null ? null : medico.getConsultorio());
        if (medico != null) {
            notificacionService.crear(
                    medico.getUsuario(),
                    TipoNotificacion.PACIENTE_EN_ESPERA,
                    "Paciente en espera",
                    paciente.getUsuario().getNombreCompleto() + " espera con el turno " + turno.getNumero() + ".",
                    "/cola");
        }
        return respuesta(turno);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoResponse> cola(EstadoTurno estado, Long medicoId, UsuarioPrincipal actor) {
        Long medicoFiltro = medicoId;
        if (esSoloMedico(actor)) {
            medicoFiltro = medicoDe(actor).getId();
        }
        Set<EstadoTurno> estados = estado == null ? EstadoTurno.ABIERTOS : EnumSet.of(estado);
        Long filtro = medicoFiltro;
        LocalDateTime ahora = LocalDateTime.now();
        return turnoRepository.findDelDia(LocalDate.now()).stream()
                .filter(turno -> estados.contains(turno.getEstado()))
                .filter(turno -> filtro == null
                        || (turno.getMedico() != null
                                && turno.getMedico().getId().equals(filtro)))
                .sorted(Comparator.comparingInt(
                                (Turno turno) -> turno.getPrioridad().orden())
                        .thenComparing(Turno::getCreadoEn))
                .map(turno -> TurnoResponse.from(turno, ahora))
                .toList();
    }

    @Override
    public TurnoResponse llamar(Long turnoId, UsuarioPrincipal actor) {
        Turno turno = obtener(turnoId);
        autorizarOperacion(turno, actor);
        exigirEstado(turno, ESTADOS_LLAMABLES, "Solo se llama a un turno en espera");
        turno.setEstado(EstadoTurno.LLAMADO);
        turno.setLlamadoEn(LocalDateTime.now());
        String lugar = turno.getConsultorio() == null || turno.getConsultorio().isBlank()
                ? "el módulo de atención"
                : turno.getConsultorio();
        notificacionService.crear(
                turno.getPaciente().getUsuario(),
                TipoNotificacion.TURNO_LLAMADO,
                "Es tu turno",
                "Turno " + turno.getNumero() + ": acércate a " + lugar + ".",
                null);
        return respuesta(turno);
    }

    @Override
    public TurnoResponse iniciar(Long turnoId, UsuarioPrincipal actor) {
        Turno turno = obtener(turnoId);
        exigirMedicoDelTurno(turno, actor);
        exigirEstado(turno, ESTADOS_INICIABLES, "Solo se inicia la atención de un turno en espera o llamado");
        turno.setEstado(EstadoTurno.EN_ATENCION);
        turno.setInicioAtencionEn(LocalDateTime.now());
        return respuesta(turno);
    }

    @Override
    public TurnoResponse finalizar(Long turnoId, UsuarioPrincipal actor) {
        Turno turno = obtener(turnoId);
        exigirMedicoDelTurno(turno, actor);
        exigirEstado(turno, EnumSet.of(EstadoTurno.EN_ATENCION), "Solo se finaliza un turno en atención");
        turno.setEstado(EstadoTurno.ATENDIDO);
        turno.setFinAtencionEn(LocalDateTime.now());
        return respuesta(turno);
    }

    @Override
    public TurnoResponse marcarAusente(Long turnoId, UsuarioPrincipal actor) {
        Turno turno = obtener(turnoId);
        autorizarOperacion(turno, actor);
        exigirEstado(turno, ESTADOS_CERRABLES, "Solo se marca ausente a un turno en espera o llamado");
        turno.setEstado(EstadoTurno.AUSENTE);
        return respuesta(turno);
    }

    @Override
    public TurnoResponse cancelar(Long turnoId, UsuarioPrincipal actor) {
        Turno turno = obtener(turnoId);
        autorizarOperacion(turno, actor);
        exigirEstado(turno, ESTADOS_CERRABLES, "Solo se cancela un turno en espera o llamado");
        turno.setEstado(EstadoTurno.CANCELADO);
        return respuesta(turno);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PantallaTurnoResponse> pantalla() {
        return turnoRepository.findDelDia(LocalDate.now()).stream()
                .filter(turno ->
                        turno.getEstado() == EstadoTurno.LLAMADO || turno.getEstado() == EstadoTurno.EN_ATENCION)
                .sorted(Comparator.comparing(Turno::getLlamadoEn, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(PANTALLA_MAXIMO)
                .map(turno -> new PantallaTurnoResponse(turno.getNumero(), turno.getConsultorio(), turno.getEstado()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecepcionResumenResponse resumen() {
        LocalDate hoy = LocalDate.now();
        List<Turno> turnos = turnoRepository.findDelDia(hoy);
        long enEspera = contar(turnos, EstadoTurno.EN_ESPERA);
        long llamados = contar(turnos, EstadoTurno.LLAMADO);
        long atendidos = contar(turnos, EstadoTurno.ATENDIDO);
        long ausentes = contar(turnos, EstadoTurno.AUSENTE);
        long esperaPromedio = Math.round(turnos.stream()
                .filter(turno -> turno.getLlamadoEn() != null || turno.getInicioAtencionEn() != null)
                .mapToLong(turno -> Duration.between(
                                turno.getCreadoEn(),
                                turno.getLlamadoEn() != null ? turno.getLlamadoEn() : turno.getInicioAtencionEn())
                        .toMinutes())
                .average()
                .orElse(0));
        Set<Long> conTurno = turnos.stream()
                .filter(turno -> turno.getCita() != null)
                .map(turno -> turno.getCita().getId())
                .collect(Collectors.toSet());
        long pendientes =
                citaRepository
                        .findAllByRango(
                                hoy.atStartOfDay(),
                                hoy.plusDays(1).atStartOfDay().minusNanos(1))
                        .stream()
                        .filter(cita -> ESTADOS_CON_LLEGADA.contains(cita.getEstado()))
                        .filter(cita -> !conTurno.contains(cita.getId()))
                        .count();
        return new RecepcionResumenResponse(
                enEspera, llamados, atendidos, ausentes, Math.max(0, esperaPromedio), pendientes);
    }

    private Turno nuevoTurno(
            Paciente paciente, Cita cita, Medico medico, PrioridadTurno prioridad, String motivo, String consultorio) {
        LocalDate hoy = LocalDate.now();
        int secuencia = turnoRepository.maxSecuencia(hoy, prioridad) + 1;
        Turno turno = Turno.builder()
                .numero(String.format("%s-%03d", prioridad.prefijo(), secuencia))
                .secuencia(secuencia)
                .fecha(hoy)
                .prioridad(prioridad)
                .estado(EstadoTurno.EN_ESPERA)
                .motivo(motivo)
                .consultorio(consultorio)
                .paciente(paciente)
                .cita(cita)
                .medico(medico)
                .creadoEn(LocalDateTime.now())
                .build();
        return turnoRepository.saveAndFlush(turno);
    }

    private Turno obtener(Long id) {
        return turnoRepository
                .findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno", "id", id));
    }

    private TurnoResponse respuesta(Turno turno) {
        return TurnoResponse.from(turno, LocalDateTime.now());
    }

    private void exigirEstado(Turno turno, Set<EstadoTurno> permitidos, String mensaje) {
        if (!permitidos.contains(turno.getEstado())) {
            throw new ConflictException(mensaje);
        }
    }

    private void autorizarOperacion(Turno turno, UsuarioPrincipal actor) {
        if (esSoloMedico(actor)) {
            exigirMedicoDelTurno(turno, actor);
        }
    }

    private void exigirMedicoDelTurno(Turno turno, UsuarioPrincipal actor) {
        if (turno.getMedico() == null || !turno.getMedico().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el médico del turno puede realizar esta operación");
        }
    }

    private boolean esSoloMedico(UsuarioPrincipal actor) {
        List<String> roles = actor.getRoles();
        return roles.contains(RolNombre.ROLE_MEDICO.name())
                && !roles.contains(RolNombre.ROLE_RECEPCION.name())
                && !roles.contains(RolNombre.ROLE_ADMIN.name());
    }

    private Medico medicoDe(UsuarioPrincipal actor) {
        return medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "email", actor.getEmail()));
    }

    private static long contar(List<Turno> turnos, EstadoTurno estado) {
        return turnos.stream().filter(turno -> turno.getEstado() == estado).count();
    }

    private static boolean coincide(Cita cita, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String termino = normalizar(q);
        return cita.getPaciente().getDni().startsWith(q.trim())
                || normalizar(cita.getPaciente().getUsuario().getNombreCompleto())
                        .contains(termino)
                || normalizar(cita.getMedico().getUsuario().getNombreCompleto()).contains(termino);
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
