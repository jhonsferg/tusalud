package com.codesoftlabs.tusalud.app.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @NotNull Tipo tipo,
        String endpoint,
        String endpointPublico,
        @NotBlank String region,
        String accessKey,
        String secretKey,
        boolean pathStyle,
        String directorioLocal,
        @NotNull Buckets buckets,
        @NotNull Duration vigenciaPrefirmada) {

    public enum Tipo {
        S3,
        LOCAL
    }

    public record Buckets(
            @NotBlank String avatares,
            @NotBlank String imagenes,
            @NotBlank String documentos,
            @NotBlank String reportes,
            @NotBlank String plantillas) {}
}
