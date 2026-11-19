package com.codesoftlabs.tusalud.app.dto.response;

public record ResumenPacienteTeleconsultaResponse(
        String pacienteNombre, Integer edad, String grupoSanguineo, String alergias, String motivo) {}
