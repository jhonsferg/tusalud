package com.codesoftlabs.tusalud.app.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.pagos")
public record PagosProperties(
        boolean simulacionHabilitada, @Positive int expiracionQrMinutos) {}
