package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.AvatarRequest;
import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.PerfilUpdateRequest;
import com.codesoftlabs.tusalud.app.dto.request.PreferenciasRequest;
import com.codesoftlabs.tusalud.app.dto.response.PerfilResponse;
import com.codesoftlabs.tusalud.app.dto.response.PreferenciasResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.UUID;

public interface PerfilService {

    PerfilResponse obtener(UsuarioPrincipal principal);

    PerfilResponse actualizar(UsuarioPrincipal principal, PerfilUpdateRequest request);

    void cambiarPassword(UsuarioPrincipal principal, PasswordChangeRequest request);

    void cambiarPassword(UsuarioPrincipal principal, PasswordChangeRequest request, UUID tokenActual);

    PreferenciasResponse obtenerPreferencias(UsuarioPrincipal principal);

    PreferenciasResponse actualizarPreferencias(UsuarioPrincipal principal, PreferenciasRequest request);

    PerfilResponse actualizarAvatar(UsuarioPrincipal principal, AvatarRequest request);

    void eliminarAvatar(UsuarioPrincipal principal);
}
