package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.DatosMedicosRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteCompartidoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final RelacionAsistencialService relacionAsistencial;
    private final AuditoriaClinicaService auditoriaClinica;
    private final UsuarioRepository usuarioRepository;

    @Override
    public HistoriaClinicaResponse obtenerMia(UsuarioPrincipal actor) {
        Paciente paciente = pacienteRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "email", actor.getEmail()));
        return construir(paciente);
    }

    @Override
    @Transactional
    public PacienteHistoriaResponse actualizarDatosMedicos(
            UUID pacienteId, UsuarioPrincipal medico, DatosMedicosRequest request) {
        relacionAsistencial.exigirRelacion(
                medico,
                pacienteId,
                "Solo puede actualizar los datos médicos de pacientes que atiende o que otro médico compartió con usted");
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", pacienteId));
        List<String> cambios = new ArrayList<>();
        String grupo = vacioANulo(request.grupoSanguineo());
        String alergias = vacioANulo(request.alergias());
        String antecedentes = vacioANulo(request.antecedentes());
        if (!Objects.equals(grupo, paciente.getGrupoSanguineo())) {
            cambios.add("grupo sanguíneo");
        }
        if (!Objects.equals(alergias, paciente.getAlergias())) {
            cambios.add("alergias");
        }
        if (!Objects.equals(antecedentes, paciente.getAntecedentes())) {
            cambios.add("antecedentes");
        }
        paciente.setGrupoSanguineo(grupo);
        paciente.setAlergias(alergias);
        paciente.setAntecedentes(antecedentes);
        paciente.setDatosMedicosModificadoPor(usuarioRepository.getReferenceById(medico.getId()));
        paciente.setDatosMedicosModificadoEn(LocalDateTime.now());
        auditoriaClinica.registrar(
                medico,
                pacienteId,
                AuditoriaClinicaService.DATOS_MEDICOS_ACTUALIZADOS,
                cambios.isEmpty() ? "sin cambios" : String.join(", ", cambios));
        return PacienteHistoriaResponse.from(paciente);
    }

    @Override
    @Transactional
    public HistoriaClinicaResponse obtenerDePaciente(UUID pacienteId, UsuarioPrincipal actor) {
        relacionAsistencial.exigirRelacion(
                actor,
                pacienteId,
                "Solo puede consultar la historia clínica de pacientes que atiende o que otro médico compartió con usted");
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", pacienteId));
        auditoriaClinica.registrar(actor, pacienteId, AuditoriaClinicaService.HISTORIA_CONSULTADA, null);
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
