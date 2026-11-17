package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Roles que tendrá el usuario")
public record RolesUsuarioRequest(
        @Schema(example = "[\"ROLE_PACIENTE\"]") @NotEmpty(message = "Indique al menos un rol")
        List<@NotNull(message = "El rol es obligatorio") RolNombre> roles) {}
