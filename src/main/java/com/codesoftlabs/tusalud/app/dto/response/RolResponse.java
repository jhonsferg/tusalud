package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rol del sistema con la cantidad de usuarios que lo tienen")
public record RolResponse(
        @Schema(example = "ROLE_ADMIN") String nombre,

        @Schema(example = "Administra la clínica") String descripcion,

        @Schema(example = "1") long usuarios) {}
