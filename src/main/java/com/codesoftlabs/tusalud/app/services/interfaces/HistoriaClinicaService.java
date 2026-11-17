package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.DatosMedicosRequest;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMedicoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface HistoriaClinicaService {

    HistoriaClinicaResponse obtenerMia(UsuarioPrincipal paciente);

    PacienteHistoriaResponse actualizarDatosMedicos(UsuarioPrincipal paciente, DatosMedicosRequest request);

    HistoriaClinicaResponse obtenerDePaciente(UUID pacienteId, UsuarioPrincipal actor);

    List<PacienteMedicoResponse> listarPacientesDeMedico(UsuarioPrincipal medico);
}
