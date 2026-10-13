package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.RegisterRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse registrarPaciente(RegisterRequest request);
}
