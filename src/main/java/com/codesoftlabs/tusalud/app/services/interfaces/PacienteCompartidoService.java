package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CompartirPacienteRequest;
import com.codesoftlabs.tusalud.app.dto.response.PacienteCompartidoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;

public interface PacienteCompartidoService {

    PacienteCompartidoResponse compartir(Long pacienteId, CompartirPacienteRequest request, UsuarioPrincipal actor);

    void revocar(Long pacienteId, Long medicoId, UsuarioPrincipal actor);

    List<PacienteCompartidoResponse> listar(Long pacienteId, UsuarioPrincipal actor);
}
