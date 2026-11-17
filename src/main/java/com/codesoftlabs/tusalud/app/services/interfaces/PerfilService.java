package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.PerfilUpdateRequest;
import com.codesoftlabs.tusalud.app.dto.response.PerfilResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;

public interface PerfilService {

    PerfilResponse obtener(UsuarioPrincipal principal);

    PerfilResponse actualizar(UsuarioPrincipal principal, PerfilUpdateRequest request);

    void cambiarPassword(UsuarioPrincipal principal, PasswordChangeRequest request);
}
