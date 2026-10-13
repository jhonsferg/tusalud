package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.HorarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.HorarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.MedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.EspecialidadRepository;
import com.codesoftlabs.tusalud.app.repositories.HorarioAtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.MedicoService;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicoServiceImpl implements MedicoService {

    private final MedicoRepository medicoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final HorarioAtencionRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<MedicoResponse> listar(Long especialidadId) {
        List<Medico> medicos;
        if (especialidadId == null) {
            medicos = medicoRepository.findAllWithDetails();
        } else {
            if (!especialidadRepository.existsById(especialidadId)) {
                throw new ResourceNotFoundException("Especialidad", "id", especialidadId);
            }
            medicos = medicoRepository.findByEspecialidadId(especialidadId);
        }
        return medicos.stream().map(MedicoResponse::from).toList();
    }

    @Override
    public MedicoResponse obtener(Long id) {
        return medicoRepository
                .findByIdWithDetails(id)
                .map(MedicoResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", id));
    }

    @Override
    @Transactional
    public MedicoResponse crear(MedicoCreateRequest request) {
        Especialidad especialidad = especialidadRepository
                .findById(request.especialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", request.especialidadId()));

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String colegiatura = request.colegiatura().trim().toUpperCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe un usuario registrado con el email " + email);
        }
        if (medicoRepository.existsByColegiaturaIgnoreCase(colegiatura)) {
            throw new ConflictException("Ya existe un médico registrado con la colegiatura " + colegiatura);
        }

        Set<Rol> roles = new HashSet<>();
        roles.add(rolRepository
                .findByNombre(RolNombre.ROLE_MEDICO)
                .orElseGet(() -> rolRepository.save(
                        Rol.builder().nombre(RolNombre.ROLE_MEDICO).build())));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .apellido(request.apellido().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .estado(EstadoUsuario.ACTIVO)
                .roles(roles)
                .build();

        Medico medico = Medico.builder()
                .colegiatura(colegiatura)
                .consultorio(
                        request.consultorio() == null
                                ? null
                                : request.consultorio().trim())
                .precioConsulta(request.precioConsulta())
                .especialidad(especialidad)
                .usuario(usuario)
                .build();
        medico = medicoRepository.save(medico);

        log.info("Médico creado: {} (id {}, {})", email, medico.getId(), colegiatura);
        return MedicoResponse.from(medico);
    }

    @Override
    public List<HorarioResponse> listarHorarios(Long medicoId) {
        verificarMedicoExiste(medicoId);
        return horarioRepository.findByMedicoId(medicoId).stream()
                .map(HorarioResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public HorarioResponse agregarHorario(Long medicoId, HorarioRequest request) {
        Medico medico = medicoRepository
                .findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", medicoId));

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new BusinessLogicException("La hora de fin debe ser posterior a la hora de inicio");
        }
        long minutosBloque =
                Duration.between(request.horaInicio(), request.horaFin()).toMinutes();
        if (minutosBloque < request.duracionCitaMinutos()) {
            throw new BusinessLogicException(String.format(
                    "El bloque de %d minutos no admite ninguna cita de %d minutos",
                    minutosBloque, request.duracionCitaMinutos()));
        }
        if (horarioRepository.existsSolapamiento(
                medicoId, request.diaSemana(), request.horaInicio(), request.horaFin())) {
            throw new ConflictException("El bloque de horario se cruza con otro bloque existente del médico ese día");
        }

        HorarioAtencion horario = HorarioAtencion.builder()
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .duracionCitaMinutos(request.duracionCitaMinutos())
                .build();
        medico.agregarHorario(horario);
        horario = horarioRepository.save(horario);

        log.info(
                "Horario agregado al médico {}: {} {}-{}",
                medicoId,
                request.diaSemana(),
                request.horaInicio(),
                request.horaFin());
        return HorarioResponse.from(horario);
    }

    @Override
    @Transactional
    public void eliminarHorario(Long medicoId, Long horarioId) {
        Medico medico = medicoRepository
                .findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", medicoId));
        HorarioAtencion horario = horarioRepository
                .findById(horarioId)
                .filter(h -> h.getMedico().getId().equals(medicoId))
                .orElseThrow(() -> new ResourceNotFoundException("Horario", "id", horarioId));

        medico.quitarHorario(horario);
        horarioRepository.delete(horario);
        log.info("Horario {} eliminado del médico {}", horarioId, medicoId);
    }

    private void verificarMedicoExiste(Long medicoId) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new ResourceNotFoundException("Médico", "id", medicoId);
        }
    }
}
