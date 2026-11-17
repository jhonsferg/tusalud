package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.ValoracionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionesMedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Valoracion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.ValoracionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ValoracionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValoracionServiceImpl implements ValoracionService {

    private final CitaRepository citaRepository;
    private final MedicoRepository medicoRepository;
    private final ValoracionRepository valoracionRepository;

    @Override
    @Transactional
    public ValoracionResponse valorar(UUID citaId, ValoracionRequest request, UsuarioPrincipal paciente) {
        Cita cita = buscarCita(citaId);
        if (!cita.getPaciente().getUsuario().getId().equals(paciente.getId())) {
            throw new ForbiddenOperationException("Solo el paciente de la cita puede valorarla");
        }
        if (cita.getEstado() != EstadoCita.ATENDIDA) {
            throw new BusinessLogicException("Solo se pueden valorar citas atendidas");
        }
        if (valoracionRepository.existsByCitaId(citaId)) {
            throw new ConflictException("La cita ya fue valorada");
        }
        Valoracion valoracion = valoracionRepository.saveAndFlush(Valoracion.builder()
                .cita(cita)
                .medico(cita.getMedico())
                .paciente(cita.getPaciente())
                .puntuacion(request.puntuacion())
                .comentario(
                        request.comentario() == null || request.comentario().isBlank()
                                ? null
                                : request.comentario().trim())
                .build());
        log.info("Cita {} valorada con {} estrellas", citaId, request.puntuacion());
        return ValoracionResponse.from(valoracion);
    }

    @Override
    public ValoracionResponse obtenerDeCita(UUID citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        boolean admin = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        boolean paciente = cita.getPaciente().getUsuario().getId().equals(actor.getId());
        boolean medico = cita.getMedico().getUsuario().getId().equals(actor.getId());
        if (!admin && !paciente && !medico) {
            throw new ForbiddenOperationException("No tiene permiso para ver la valoración de esta cita");
        }
        return valoracionRepository
                .findByCitaIdConDetalle(citaId)
                .map(ValoracionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Valoración", "citaId", citaId));
    }

    @Override
    public ValoracionesMedicoResponse listarDeMedico(UUID medicoId) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new ResourceNotFoundException("Médico", "id", medicoId);
        }
        List<ValoracionResponse> valoraciones = valoracionRepository.findByMedicoIdConDetalle(medicoId).stream()
                .map(ValoracionResponse::from)
                .toList();
        double promedio = valoraciones.isEmpty()
                ? 0
                : BigDecimal.valueOf(valoraciones.stream()
                                .mapToInt(ValoracionResponse::puntuacion)
                                .average()
                                .orElse(0))
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
        return new ValoracionesMedicoResponse(promedio, valoraciones.size(), valoraciones);
    }

    private Cita buscarCita(UUID citaId) {
        return citaRepository
                .findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
    }
}
