package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.MfaVerificarRequest;
import com.codesoftlabs.tusalud.app.dto.request.RefreshTokenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RegisterRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import java.util.UUID;

public interface AuthService {

    AuthResponse login(LoginRequest request, ContextoConexion contexto);

    AuthResponse verificarMfa(MfaVerificarRequest request, ContextoConexion contexto);

    AuthResponse registrarPaciente(RegisterRequest request, ContextoConexion contexto);

    AuthResponse refrescar(RefreshTokenRequest request);

    void cerrarSesion(UUID tokenId);
}
