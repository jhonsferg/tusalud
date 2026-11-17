package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.DatosMedicosRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteCompartidoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {

    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final AtencionRepository atencionRepository;
    private final PacienteCompartidoRepository compartidoRepository;

    @Override
    public HistoriaClinicaResponse obtenerMia(UsuarioPrincipal actor) {
        Paciente paciente = pacienteRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "email", actor.getEmail()));
        return construir(paciente);
    }

    @Override
    @Transactional
    public PacienteHistoriaResponse actualizarDatosMedicos(UsuarioPrincipal actor, DatosMedicosRequest request) {
        Paciente paciente = pacienteRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "email", actor.getEmail()));
        paciente.setGrupoSanguineo(vacioANulo(request.grupoSanguineo()));
        paciente.setAlergias(vacioANulo(request.alergias()));
        paciente.setAntecedentes(vacioANulo(request.antecedentes()));
        return PacienteHistoriaResponse.from(paciente);
    }

    @Override
    public HistoriaClinicaResponse obtenerDePaciente(UUID pacienteId, UsuarioPrincipal actor) {
        boolean admin = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        boolean acceso = citaRepository.existsRelacionMedicoPaciente(actor.getId(), pacienteId)
                || compartidoRepository.existsParaUsuario(pacienteId, actor.getId());
        if (!admin && !acceso) {
            throw new ForbiddenOperationException(
                    "Solo puede consultar la historia clínica de pacientes que atiende o que otro médico compartió con usted");
        }
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", pacienteId));
        return construir(paciente);
    }

    @Override
    public List<PacienteMedicoResponse> listarPacientesDeMedico(UsuarioPrincipal medico) {
        Map<UUID, List<Cita>> citasPorPaciente = new LinkedHashMap<>();
        for (Cita cita : citaRepository.findDelMedicoConPacientes(medico.getId())) {
            citasPorPaciente
                    .computeIfAbsent(cita.getPaciente().getId(), id -> new ArrayList<>())
                    .add(cita);
        }
        List<PacienteMedicoResponse> propios = citasPorPaciente.values().stream()
                .map(citas -> {
                    Paciente paciente = citas.get(0).getPaciente();
                    return new PacienteMedicoResponse(
                            paciente.getId(),
                            paciente.getUsuario().getNombreCompleto(),
                            paciente.getDni(),
                            paciente.getHistorialClinicoNumero(),
                            citas.stream()
                                    .map(Cita::getFechaHora)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(null),
                            citas.size(),
                            null);
                })
                .toList();
        List<PacienteMedicoResponse> todos = new ArrayList<>(propios);
        for (PacienteCompartido compartido : compartidoRepository.findParaUsuarioConDetalle(medico.getId())) {
            Paciente paciente = compartido.getPaciente();
            if (citasPorPaciente.containsKey(paciente.getId())) {
                continue;
            }
            todos.add(new PacienteMedicoResponse(
                    paciente.getId(),
                    paciente.getUsuario().getNombreCompleto(),
                    paciente.getDni(),
                    paciente.getHistorialClinicoNumero(),
                    null,
                    0,
                    compartido.getMedicoOrigen().getUsuario().getNombreCompleto()));
        }
        return todos.stream()
                .sorted(Comparator.comparing(PacienteMedicoResponse::nombreCompleto))
                .toList();
    }

    private HistoriaClinicaResponse construir(Paciente paciente) {
        List<AtencionResponse> atenciones = atencionRepository.findByPacienteIdConDetalle(paciente.getId()).stream()
                .map(AtencionResponse::from)
                .toList();
        return new HistoriaClinicaResponse(PacienteHistoriaResponse.from(paciente), atenciones);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
