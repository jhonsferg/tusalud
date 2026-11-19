package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.config.TelemedicinaProperties;
import com.codesoftlabs.tusalud.app.dto.response.ConsentimientoTelemedicinaResponse;
import com.codesoftlabs.tusalud.app.dto.response.IceServersResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenPacienteTeleconsultaResponse;
import com.codesoftlabs.tusalud.app.dto.response.TeleconsultaResponse;
import com.codesoftlabs.tusalud.app.dto.response.TicketTeleconsultaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.ConsentimientoTelemedicina;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Teleconsulta;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTeleconsulta;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoTelemedicinaRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.TeleconsultaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.TeleconsultaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeleconsultaServiceImpl implements TeleconsultaService {

    static final String ROL_MEDICO = "MEDICO";
    static final String ROL_PACIENTE = "PACIENTE";
    static final String RUTA_SENALIZACION = "/ws/teleconsulta";
    static final String TEXTO_CONSENTIMIENTO = "Acepto recibir atención médica a distancia mediante videollamada. "
            + "Entiendo que la teleconsulta no sustituye una atención presencial cuando el médico la considere necesaria, "
            + "que el audio y el video se transmiten directamente entre el médico y yo y que TuSalud no los graba, "
            + "que el chat de la sala no se conserva y que mis datos clínicos se tratan con la misma confidencialidad "
            + "y auditoría que en la consulta presencial.";
    private static final Set<EstadoTeleconsulta> ACTIVAS =
            EnumSet.of(EstadoTeleconsulta.PROGRAMADA, EstadoTeleconsulta.SALA_ABIERTA, EstadoTeleconsulta.EN_CURSO);
    private static final int CIERRE_AUTOMATICO_MINUTOS = 90;

    private final TeleconsultaRepository teleconsultaRepository;
    private final ConsentimientoTelemedicinaRepository consentimientoRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaResponseMapper citaMapper;
    private final NotificacionService notificacionService;
    private final AuditoriaClinicaService auditoriaClinicaService;
    private final EventoSeguridadService eventoSeguridadService;
    private final TicketTeleconsultaService ticketService;
    private final TurnCredencialesService turnService;
    private final TelemedicinaProperties propiedades;
    private final ApplicationEventPublisher eventos;

    @Override
    @Transactional
    public void crearParaCita(Cita cita) {
        if (teleconsultaRepository.findByCitaId(cita.getId()).isPresent()) {
            return;
        }
        teleconsultaRepository.save(
                Teleconsulta.builder().cita(cita).sala(UUID.randomUUID()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeleconsultaResponse> listarMias(UsuarioPrincipal actor) {
        return teleconsultaRepository.findDelPaciente(actor.getId()).stream()
                .map(t -> mapear(t, ROL_PACIENTE, actor))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeleconsultaResponse> listarDelMedico(UsuarioPrincipal actor, LocalDate desde) {
        LocalDate inicio = desde == null ? LocalDate.now() : desde;
        return teleconsultaRepository.findDelMedico(actor.getId(), inicio.atStartOfDay()).stream()
                .map(t -> mapear(t, ROL_MEDICO, actor))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeleconsultaResponse obtener(UUID id, UsuarioPrincipal actor) {
        Teleconsulta t = cargar(id);
        return mapear(t, participante(t, actor), actor);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentimientoTelemedicinaResponse consentimiento(UsuarioPrincipal actor) {
        Paciente paciente = pacienteDe(actor);
        String version = propiedades.versionConsentimiento();
        boolean aceptado = consentimientoRepository.existsByPacienteIdAndVersion(paciente.getId(), version);
        return new ConsentimientoTelemedicinaResponse(version, TEXTO_CONSENTIMIENTO, aceptado, null);
    }

    @Override
    @Transactional
    public ConsentimientoTelemedicinaResponse aceptarConsentimiento(UsuarioPrincipal actor, String version) {
        Paciente paciente = pacienteDe(actor);
        if (!propiedades.versionConsentimiento().equals(version)) {
            throw new DatosInvalidosException("La versión del consentimiento no es la vigente");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (!consentimientoRepository.existsByPacienteIdAndVersion(paciente.getId(), version)) {
            consentimientoRepository.save(ConsentimientoTelemedicina.builder()
                    .paciente(paciente)
                    .version(version)
                    .aceptadoEn(ahora)
                    .build());
            auditoriaClinicaService.registrar(
                    actor, paciente.getId(), "CONSENTIMIENTO_TELEMEDICINA", "Versión " + version);
        }
        return new ConsentimientoTelemedicinaResponse(version, TEXTO_CONSENTIMIENTO, true, ahora);
    }

    @Override
    @Transactional
    public TeleconsultaResponse abrir(UUID id, UsuarioPrincipal actor) {
        Teleconsulta t = cargar(id);
        exigirRol(t, actor, ROL_MEDICO);
        exigirIngresable(t);
        abrirSala(t);
        return mapear(t, ROL_MEDICO, actor);
    }

    @Override
    @Transactional
    public TicketTeleconsultaResponse emitirTicket(UUID id, UsuarioPrincipal actor) {
        Teleconsulta t = cargar(id);
        String rol = participante(t, actor);
        exigirIngresable(t);
        if (ROL_PACIENTE.equals(rol) && consentimientoPendiente(t)) {
            throw new BusinessLogicException("Debe aceptar el consentimiento de telemedicina antes de ingresar");
        }
        String ticket = ticketService.emitir(t.getId(), actor.getId());
        return new TicketTeleconsultaResponse(ticket, propiedades.ticketTtlSeg(), RUTA_SENALIZACION);
    }

    @Override
    @Transactional(readOnly = true)
    public IceServersResponse ice(UUID id, UsuarioPrincipal actor) {
        Teleconsulta t = cargar(id);
        participante(t, actor);
        return turnService.generar(actor.getId());
    }

    @Override
    @Transactional
    public TeleconsultaResponse finalizar(UUID id, UsuarioPrincipal actor) {
        Teleconsulta t = cargar(id);
        exigirRol(t, actor, ROL_MEDICO);
        if (!ACTIVAS.contains(t.getEstado())) {
            throw new BusinessLogicException("La teleconsulta ya no está activa");
        }
        cerrar(t);
        eventos.publishEvent(new TeleconsultaFinalizadaEvento(t.getId()));
        return mapear(t, ROL_MEDICO, actor);
    }

    @Override
    @Transactional
    public ResumenPacienteTeleconsultaResponse resumenPaciente(UUID id, UsuarioPrincipal actor) {
        Teleconsulta t = cargar(id);
        exigirRol(t, actor, ROL_MEDICO);
        Paciente paciente = t.getCita().getPaciente();
        auditoriaClinicaService.registrar(
                actor, paciente.getId(), "TELECONSULTA_RESUMEN", "Resumen mínimo durante la teleconsulta");
        Integer edad = paciente.getFechaNacimiento() == null
                ? null
                : Period.between(paciente.getFechaNacimiento(), LocalDate.now()).getYears();
        return new ResumenPacienteTeleconsultaResponse(
                paciente.getUsuario().getNombreCompleto(),
                edad,
                paciente.getGrupoSanguineo(),
                paciente.getAlergias(),
                t.getCita().getMotivo());
    }

    @Override
    @Transactional
    public IngresoSala alConectar(UUID teleconsultaId, UUID usuarioId) {
        Teleconsulta t = cargar(teleconsultaId);
        String rol = rolDe(t, usuarioId);
        if (rol == null) {
            eventoSeguridadService.registrarAccesoDenegado("WS", "/ws/teleconsulta");
            throw new ForbiddenOperationException("No participa en esta teleconsulta");
        }
        exigirIngresable(t);
        if (ROL_MEDICO.equals(rol)) {
            abrirSala(t);
        } else if (!t.isPacienteEnSalaNotificado()) {
            t.setPacienteEnSalaNotificado(true);
            notificacionService.crear(
                    t.getCita().getMedico().getUsuario(),
                    TipoNotificacion.TELECONSULTA_PACIENTE_EN_SALA,
                    "Paciente en la sala de espera",
                    t.getCita().getPaciente().getUsuario().getNombreCompleto() + " espera en la teleconsulta.",
                    "/teleconsultas/" + t.getId() + "/sala");
        }
        return new IngresoSala(rol, t.getEstado() == EstadoTeleconsulta.EN_CURSO);
    }

    @Override
    @Transactional
    public void alAdmitir(UUID teleconsultaId) {
        Teleconsulta t = cargar(teleconsultaId);
        if (t.getEstado() != EstadoTeleconsulta.EN_CURSO) {
            t.setEstado(EstadoTeleconsulta.EN_CURSO);
            if (t.getInicioReal() == null) {
                t.setInicioReal(LocalDateTime.now());
            }
        }
    }

    @Override
    @Transactional
    public void alFinalizarDesdeSala(UUID teleconsultaId) {
        Teleconsulta t = cargar(teleconsultaId);
        if (ACTIVAS.contains(t.getEstado())) {
            cerrar(t);
        }
    }

    @Override
    @Transactional
    public void alCalidad(UUID teleconsultaId, int reconexiones, String candidato) {
        Teleconsulta t = cargar(teleconsultaId);
        t.setReconexiones(Math.max(t.getReconexiones(), Math.min(reconexiones, 50)));
        if (candidato != null && Set.of("host", "srflx", "prflx", "relay").contains(candidato)) {
            t.setTipoCandidato(candidato);
        }
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    @Transactional
    public void enviarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now();
        for (Teleconsulta t : teleconsultaRepository.findParaRecordar(
                EnumSet.of(EstadoTeleconsulta.PROGRAMADA, EstadoTeleconsulta.SALA_ABIERTA),
                ahora,
                ahora.plusMinutes(15))) {
            t.setRecordatorioEnviado(true);
            String enlace = "/teleconsultas/" + t.getId() + "/prueba";
            notificacionService.crear(
                    t.getCita().getPaciente().getUsuario(),
                    TipoNotificacion.TELECONSULTA_PROXIMA,
                    "Su teleconsulta comienza pronto",
                    "Su teleconsulta con "
                            + t.getCita().getMedico().getUsuario().getNombreCompleto() + " empieza en unos minutos.",
                    enlace);
            notificacionService.crear(
                    t.getCita().getMedico().getUsuario(),
                    TipoNotificacion.TELECONSULTA_PROXIMA,
                    "Teleconsulta próxima",
                    "Teleconsulta con " + t.getCita().getPaciente().getUsuario().getNombreCompleto()
                            + " en unos minutos.",
                    "/teleconsultas/" + t.getId() + "/sala");
        }
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 45_000)
    @Transactional
    public void cerrarVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.minusMinutes(propiedades.ventanaDespuesMin());
        for (Teleconsulta t : teleconsultaRepository.findVencidas(ACTIVAS, limite)) {
            boolean iniciada = t.getEstado() == EstadoTeleconsulta.EN_CURSO;
            if (!iniciada) {
                t.setEstado(EstadoTeleconsulta.NO_ASISTIO);
                eventos.publishEvent(new TeleconsultaFinalizadaEvento(t.getId()));
            } else if (t.getCita().getFechaHora().isBefore(ahora.minusMinutes(CIERRE_AUTOMATICO_MINUTOS))) {
                cerrar(t);
                eventos.publishEvent(new TeleconsultaFinalizadaEvento(t.getId()));
            }
        }
    }

    private void abrirSala(Teleconsulta t) {
        if (t.getEstado() == EstadoTeleconsulta.PROGRAMADA) {
            t.setEstado(EstadoTeleconsulta.SALA_ABIERTA);
            t.setAbiertaEn(LocalDateTime.now());
        }
        if (!t.isSalaAbiertaNotificada()) {
            t.setSalaAbiertaNotificada(true);
            notificacionService.crear(
                    t.getCita().getPaciente().getUsuario(),
                    TipoNotificacion.TELECONSULTA_SALA_ABIERTA,
                    "Su médico abrió la sala",
                    "Puede ingresar a la teleconsulta con "
                            + t.getCita().getMedico().getUsuario().getNombreCompleto() + ".",
                    "/teleconsultas/" + t.getId() + "/prueba");
        }
    }

    private void cerrar(Teleconsulta t) {
        LocalDateTime ahora = LocalDateTime.now();
        t.setEstado(EstadoTeleconsulta.FINALIZADA);
        t.setFinReal(ahora);
        if (t.getInicioReal() != null) {
            t.setDuracionSegundos((int) Math.max(0, ChronoUnit.SECONDS.between(t.getInicioReal(), ahora)));
        }
    }

    private void exigirIngresable(Teleconsulta t) {
        if (!ingresable(t)) {
            EstadoTeleconsulta estado = estadoEfectivo(t);
            if (!ACTIVAS.contains(estado)) {
                throw new BusinessLogicException("La teleconsulta ya no está disponible");
            }
            LocalDateTime desde = ventanaDesde(t);
            if (LocalDateTime.now().isBefore(desde)) {
                throw new BusinessLogicException("La sala se habilita " + propiedades.ventanaAntesMin()
                        + " minutos antes de la hora de la teleconsulta");
            }
            throw new BusinessLogicException("La ventana de ingreso de la teleconsulta ya terminó");
        }
    }

    private boolean ingresable(Teleconsulta t) {
        LocalDateTime ahora = LocalDateTime.now();
        return ACTIVAS.contains(estadoEfectivo(t))
                && !ahora.isBefore(ventanaDesde(t))
                && !ahora.isAfter(ventanaHasta(t));
    }

    private LocalDateTime ventanaDesde(Teleconsulta t) {
        return t.getCita().getFechaHora().minusMinutes(propiedades.ventanaAntesMin());
    }

    private LocalDateTime ventanaHasta(Teleconsulta t) {
        return t.getCita().getFechaHora().plusMinutes(propiedades.ventanaDespuesMin());
    }

    private EstadoTeleconsulta estadoEfectivo(Teleconsulta t) {
        if (t.getCita().getEstado() == EstadoCita.CANCELADA && t.getEstado() != EstadoTeleconsulta.FINALIZADA) {
            return EstadoTeleconsulta.CANCELADA;
        }
        return t.getEstado();
    }

    private boolean consentimientoPendiente(Teleconsulta t) {
        return !consentimientoRepository.existsByPacienteIdAndVersion(
                t.getCita().getPaciente().getId(), propiedades.versionConsentimiento());
    }

    private TeleconsultaResponse mapear(Teleconsulta t, String rol, UsuarioPrincipal actor) {
        Cita cita = t.getCita();
        EstadoTeleconsulta estado = estadoEfectivo(t);
        return new TeleconsultaResponse(
                t.getId(),
                cita.getId(),
                cita.getFechaHora(),
                citaMapper.duracionDe(cita),
                estado,
                cita.getMedico().getUsuario().getNombreCompleto(),
                cita.getEspecialidad().getNombre(),
                cita.getPaciente().getUsuario().getNombreCompleto(),
                cita.getMotivo(),
                rol,
                ventanaDesde(t),
                ventanaHasta(t),
                ingresable(t),
                estado == EstadoTeleconsulta.SALA_ABIERTA || estado == EstadoTeleconsulta.EN_CURSO,
                ROL_PACIENTE.equals(rol) && consentimientoPendiente(t),
                t.getDuracionSegundos());
    }

    private Teleconsulta cargar(UUID id) {
        return teleconsultaRepository
                .findDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teleconsulta", "id", id));
    }

    private String rolDe(Teleconsulta t, UUID usuarioId) {
        if (t.getCita().getMedico().getUsuario().getId().equals(usuarioId)) {
            return ROL_MEDICO;
        }
        if (t.getCita().getPaciente().getUsuario().getId().equals(usuarioId)) {
            return ROL_PACIENTE;
        }
        return null;
    }

    private String participante(Teleconsulta t, UsuarioPrincipal actor) {
        String rol = rolDe(t, actor.getId());
        if (rol == null) {
            eventoSeguridadService.registrarAccesoDenegado("GET", "/api/v1/teleconsultas/" + t.getId());
            throw new ForbiddenOperationException("No participa en esta teleconsulta");
        }
        return rol;
    }

    private void exigirRol(Teleconsulta t, UsuarioPrincipal actor, String requerido) {
        if (!requerido.equals(participante(t, actor))) {
            throw new ForbiddenOperationException("Esta acción corresponde al " + requerido.toLowerCase());
        }
    }

    private Paciente pacienteDe(UsuarioPrincipal actor) {
        return pacienteRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "email", actor.getEmail()));
    }
}
