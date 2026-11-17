package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CompartirPacienteRequest;
import com.codesoftlabs.tusalud.app.dto.response.PacienteCompartidoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface PacienteCompartidoService {

    PacienteCompartidoResponse compartir(UUID pacienteId, CompartirPacienteRequest request, UsuarioPrincipal actor);

    void revocar(UUID pacienteId, UUID medicoId, UsuarioPrincipal actor);

    List<PacienteCompartidoResponse> listar(UUID pacienteId, UsuarioPrincipal actor);
}
