package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CompartirPacienteRequest;
import com.codesoftlabs.tusalud.app.dto.response.PacienteCompartidoResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteCompartidoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PacienteCompartidoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PacienteCompartidoServiceImpl implements PacienteCompartidoService {

    private final PacienteCompartidoRepository compartidoRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final CitaRepository citaRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public PacienteCompartidoResponse compartir(
            Long pacienteId, CompartirPacienteRequest request, UsuarioPrincipal actor) {
        Medico origen = medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un médico puede compartir pacientes"));
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", pacienteId));
        if (!citaRepository.existsRelacionMedicoPaciente(actor.getId(), pacienteId)) {
            throw new ForbiddenOperationException(
                    "Solo puede compartir pacientes que tienen o tuvieron citas con usted");
        }
        Medico destino = medicoRepository
                .findByIdWithDetails(request.medicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", request.medicoId()));
        if (destino.getId().equals(origen.getId())) {
            throw new BusinessLogicException("No puede compartir un paciente consigo mismo");
        }
        if (!destino.getUsuario().isActivo()) {
            throw new BusinessLogicException("El médico seleccionado no se encuentra activo");
        }
        if (compartidoRepository.existsByPacienteIdAndMedicoDestinoId(pacienteId, destino.getId())) {
            throw new ConflictException("El paciente ya fue compartido con ese médico");
        }

        PacienteCompartido compartido = compartidoRepository.saveAndFlush(PacienteCompartido.builder()
                .paciente(paciente)
                .medicoOrigen(origen)
                .medicoDestino(destino)
                .nota(vacioANulo(request.nota()))
                .build());

        notificacionService.crear(
                destino.getUsuario(),
                TipoNotificacion.PACIENTE_COMPARTIDO,
                "Paciente compartido contigo",
                origen.getUsuario().getNombreCompleto() + " compartió contigo a "
                        + paciente.getUsuario().getNombreCompleto() + ".",
                "/pacientes/" + pacienteId + "/historia-clinica");
        log.info(
                "Paciente {} compartido por el médico {} con el médico {}",
                pacienteId,
                origen.getId(),
                destino.getId());
        return PacienteCompartidoResponse.from(compartido);
    }

    @Override
    @Transactional
    public void revocar(Long pacienteId, Long medicoId, UsuarioPrincipal actor) {
        PacienteCompartido compartido = compartidoRepository
                .findByPacienteIdAndMedicoDestinoId(pacienteId, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente compartido", "id", pacienteId));
        boolean admin = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        boolean esOrigen = compartido.getMedicoOrigen().getUsuario().getId().equals(actor.getId());
        if (!admin && !esOrigen) {
            throw new ForbiddenOperationException(
                    "Solo el médico que compartió al paciente o un administrador pueden revocar el acceso");
        }
        compartidoRepository.delete(compartido);
        log.info("Acceso compartido del paciente {} al médico {} revocado por {}", pacienteId, medicoId, actor.getId());
    }

    @Override
    public List<PacienteCompartidoResponse> listar(Long pacienteId, UsuarioPrincipal actor) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new ResourceNotFoundException("Paciente", "id", pacienteId);
        }
        boolean admin = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        boolean acceso = citaRepository.existsRelacionMedicoPaciente(actor.getId(), pacienteId)
                || compartidoRepository.existsParaUsuario(pacienteId, actor.getId());
        if (!admin && !acceso) {
            throw new ForbiddenOperationException("No tiene acceso a este paciente");
        }
        return compartidoRepository.findByPacienteIdConDetalle(pacienteId).stream()
                .map(PacienteCompartidoResponse::from)
                .toList();
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
