package com.codesoftlabs.tusalud.app.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.respaldos")
public record RespaldosProperties(
        String token,
        boolean vigilanciaHabilitada,
        @Positive int maxHorasSinRespaldo,
        @Positive int maxDiasSinPrueba,
        @Positive int rpoObjetivoHoras,
        @Positive int rtoObjetivoMinutos) {}
