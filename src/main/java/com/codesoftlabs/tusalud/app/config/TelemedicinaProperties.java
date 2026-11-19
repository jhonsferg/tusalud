package com.codesoftlabs.tusalud.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.telemedicina")
public record TelemedicinaProperties(
        @Positive long ventanaAntesMin,
        @Positive long ventanaDespuesMin,
        @Positive int ticketTtlSeg,
        @NotBlank String versionConsentimiento,
        @Positive int maxMensajesPorVentana,
        @Positive int inactividadSeg,
        @NotNull @Valid Turn turn) {

    public record Turn(
            boolean habilitado,
            @NotBlank String host,
            @Positive int puerto,
            @NotBlank String secreto,
            @Positive int ttlSeg) {}
}
