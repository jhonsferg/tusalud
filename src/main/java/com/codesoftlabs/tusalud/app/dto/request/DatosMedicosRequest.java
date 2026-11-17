package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos médicos básicos del paciente autenticado")
public record DatosMedicosRequest(
        @Schema(example = "O+", description = "Uno de A+, A-, B+, B-, AB+, AB-, O+, O- (vacío para limpiar)")
        @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "El grupo sanguíneo no es válido")
        String grupoSanguineo,

        @Schema(example = "Penicilina") @Size(max = 1000, message = "Las alergias no deben superar 1000 caracteres")
        String alergias,

        @Schema(example = "Hipertensión familiar")
        @Size(max = 2000, message = "Los antecedentes no deben superar 2000 caracteres")
        String antecedentes) {}
