package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.DatosMedicosRequest;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMedicoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;

public interface HistoriaClinicaService {

    HistoriaClinicaResponse obtenerMia(UsuarioPrincipal paciente);

    PacienteHistoriaResponse actualizarDatosMedicos(UsuarioPrincipal paciente, DatosMedicosRequest request);

    HistoriaClinicaResponse obtenerDePaciente(Long pacienteId, UsuarioPrincipal actor);

    List<PacienteMedicoResponse> listarPacientesDeMedico(UsuarioPrincipal medico);
}
