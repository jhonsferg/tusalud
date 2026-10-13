package com.codesoftlabs.tusalud.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI tusaludOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TuSalud API - Sistema de Gestión de Citas y Consultas Médicas")
                        .description("""
                                API REST para la gestión de especialidades, médicos, horarios de atención y citas.
                                Autenticación stateless con JWT: obtenga un token en /api/v1/auth/login y
                                use el botón 'Authorize' con el valor del token.""")
                        .version("v1")
                        .contact(new Contact().name("Equipo TuSalud"))
                        .license(new License().name("Uso académico")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
