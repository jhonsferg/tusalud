package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Códigos de recuperación de un solo uso; solo se muestran una vez")
public record MfaCodigosRecuperacionResponse(
        @Schema(example = "[\"A1B2-C3D4\"]") List<String> codigosRecuperacion) {}
