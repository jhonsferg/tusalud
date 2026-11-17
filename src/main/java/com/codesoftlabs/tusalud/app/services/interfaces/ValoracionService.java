package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.ValoracionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionesMedicoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;

public interface ValoracionService {

    ValoracionResponse valorar(Long citaId, ValoracionRequest request, UsuarioPrincipal paciente);

    ValoracionResponse obtenerDeCita(Long citaId, UsuarioPrincipal actor);

    ValoracionesMedicoResponse listarDeMedico(Long medicoId);
}
