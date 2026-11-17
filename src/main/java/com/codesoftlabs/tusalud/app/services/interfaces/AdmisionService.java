package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CitaMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.request.PacienteMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMostradorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteRegistradoResponse;
import java.util.List;

public interface AdmisionService {

    List<PacienteMostradorResponse> buscarPacientes(String q);

    PacienteRegistradoResponse registrarPaciente(PacienteMostradorRequest request);

    CitaResponse reservarCita(CitaMostradorRequest request);
}
