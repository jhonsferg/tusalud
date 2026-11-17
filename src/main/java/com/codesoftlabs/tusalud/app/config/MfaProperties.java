package com.codesoftlabs.tusalud.app.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mfa")
public record MfaProperties(@NotBlank String claveCifrado) {}
