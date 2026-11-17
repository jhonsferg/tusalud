package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.UUID;

public interface AtencionService {

    AtencionResponse registrar(UUID citaId, AtencionRequest request, UsuarioPrincipal medico);

    AtencionResponse actualizar(UUID citaId, AtencionRequest request, UsuarioPrincipal medico);

    AtencionResponse obtener(UUID citaId, UsuarioPrincipal actor);
}
