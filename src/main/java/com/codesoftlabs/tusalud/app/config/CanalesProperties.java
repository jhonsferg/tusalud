package com.codesoftlabs.tusalud.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.canales")
public record CanalesProperties(
        boolean workerHabilitado,
        @NotBlank String urlBase,
        @NotBlank String secreto,
        String webhookSecreto,
        @NotBlank String centro,
        @Positive int limitePorHora,
        @NotNull @Valid Reintentos reintentos,
        @NotNull @Valid Codigo codigo,
        @NotNull @Valid Correo correo,
        @NotNull @Valid Mensajeria sms,
        @NotNull @Valid Mensajeria whatsapp) {

    public record Reintentos(
            @Positive int maximo,
            @Positive int baseSeg,
            @Positive int techoSeg) {}

    public record Codigo(
            @Positive int ttlMin,
            @Positive int maxIntentos,
            @Positive int reenviosPorHora,
            @Positive int esperaSeg) {}

    public record Correo(
            @NotBlank String proveedor,
            @NotBlank String remitente,
            @NotBlank String nombreRemitente) {

        public boolean smtp() {
            return "smtp".equalsIgnoreCase(proveedor);
        }
    }

    public record Mensajeria(
            @NotBlank String proveedor, @Valid Http http) {

        public boolean simulado() {
            return !"http".equalsIgnoreCase(proveedor);
        }
    }

    public record Http(
            String url,
            String autenticacion,
            String usuario,
            String secreto,
            String formato,
            String cuerpo,
            String campoIdExterno,
            Integer tiempoLimiteSeg) {}
}
