package com.codesoftlabs.tusalud.app.documentos;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.documentos")
public record DocumentosProperties(
        Duration retencionExportaciones, Integer maximoConcurrentes, Integer maximoPorHora, Boolean sincronico) {

    public DocumentosProperties {
        retencionExportaciones = retencionExportaciones == null ? Duration.ofDays(7) : retencionExportaciones;
        maximoConcurrentes = maximoConcurrentes == null ? 3 : maximoConcurrentes;
        maximoPorHora = maximoPorHora == null ? 20 : maximoPorHora;
        sincronico = sincronico != null && sincronico;
    }
}
