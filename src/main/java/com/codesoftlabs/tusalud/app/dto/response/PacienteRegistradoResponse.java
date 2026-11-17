package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paciente registrado en el mostrador con su contraseña temporal, que solo se entrega esta vez")
public record PacienteRegistradoResponse(
        PacienteMostradorResponse paciente,
        @Schema(example = "Xk7mQ2pRt9") String passwordTemporal) {}
