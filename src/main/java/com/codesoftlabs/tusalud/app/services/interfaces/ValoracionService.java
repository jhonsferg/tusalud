package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.ValoracionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionesMedicoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.UUID;

public interface ValoracionService {

    ValoracionResponse valorar(UUID citaId, ValoracionRequest request, UsuarioPrincipal paciente);

    ValoracionResponse obtenerDeCita(UUID citaId, UsuarioPrincipal actor);

    ValoracionesMedicoResponse listarDeMedico(UUID medicoId);
}
