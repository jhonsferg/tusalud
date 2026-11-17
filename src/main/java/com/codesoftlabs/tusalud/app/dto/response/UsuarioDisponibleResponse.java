package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;
import java.util.UUID;

public record UsuarioDisponibleResponse(UUID usuarioId, String nombreCompleto, String email, List<String> roles) {}
