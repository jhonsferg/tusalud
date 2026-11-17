package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;

public interface AtencionService {

    AtencionResponse registrar(Long citaId, AtencionRequest request, UsuarioPrincipal medico);

    AtencionResponse actualizar(Long citaId, AtencionRequest request, UsuarioPrincipal medico);

    AtencionResponse obtener(Long citaId, UsuarioPrincipal actor);
}
