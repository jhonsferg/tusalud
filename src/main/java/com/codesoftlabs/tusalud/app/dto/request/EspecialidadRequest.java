package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar una especialidad")
public record EspecialidadRequest(
        @Schema(example = "Cardiología")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @Schema(example = "Diagnóstico y tratamiento de enfermedades del corazón")
        @Size(max = 500, message = "La descripción no debe superar 500 caracteres")
        String descripcion) {}
