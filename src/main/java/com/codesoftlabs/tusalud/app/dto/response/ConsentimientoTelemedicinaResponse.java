package com.codesoftlabs.tusalud.app.dto.response;

import java.time.LocalDateTime;

public record ConsentimientoTelemedicinaResponse(
        String version, String texto, boolean aceptado, LocalDateTime aceptadoEn) {}
