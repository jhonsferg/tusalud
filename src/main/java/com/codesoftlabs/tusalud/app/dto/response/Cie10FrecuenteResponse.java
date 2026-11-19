package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Diagnóstico que el médico registra con más frecuencia")
public record Cie10FrecuenteResponse(String codigo, String descripcion, long casos) {}
