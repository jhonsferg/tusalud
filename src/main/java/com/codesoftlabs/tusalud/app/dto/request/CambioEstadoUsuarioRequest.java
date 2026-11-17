package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo estado de una cuenta de usuario")
public record CambioEstadoUsuarioRequest(
        @Schema(example = "INACTIVO") @NotNull(message = "El estado es obligatorio")
        EstadoUsuario estado) {}
