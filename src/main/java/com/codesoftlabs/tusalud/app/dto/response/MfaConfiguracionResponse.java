package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Secreto y URI para registrar la cuenta en una aplicación autenticadora")
public record MfaConfiguracionResponse(
        @Schema(example = "JBSWY3DPEHPK3PXP") String secreto,

        @Schema(example = "otpauth://totp/TuSalud:juan.perez@correo.com?secret=JBSWY3DPEHPK3PXP&issuer=TuSalud")
        String otpauthUri) {}
