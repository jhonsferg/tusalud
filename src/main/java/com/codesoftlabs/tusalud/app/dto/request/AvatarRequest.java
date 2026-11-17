package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Imagen de perfil codificada como data URL (PNG, JPEG o WEBP, máximo 300 KB)")
public record AvatarRequest(
        @Schema(example = "data:image/png;base64,iVBORw0KGgo=") @NotBlank(message = "La imagen es obligatoria")
        String imagen) {}
