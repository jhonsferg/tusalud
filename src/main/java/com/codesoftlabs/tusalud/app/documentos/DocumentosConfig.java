package com.codesoftlabs.tusalud.app.documentos;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentosConfig {

    @Bean(name = "exportacionExecutor", destroyMethod = "")
    Executor exportacionExecutor(DocumentosProperties propiedades) {
        if (propiedades.sincronico()) {
            return Runnable::run;
        }
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
