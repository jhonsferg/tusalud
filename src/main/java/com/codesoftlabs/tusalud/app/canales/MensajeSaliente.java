package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;

public record MensajeSaliente(
        Canal canal,
        String destino,
        String asunto,
        String texto,
        String html,
        String enlaceBaja,
        String claveIdempotencia) {}
