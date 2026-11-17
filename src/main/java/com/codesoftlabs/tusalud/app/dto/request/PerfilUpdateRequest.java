package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Datos personales editables del usuario autenticado")
public record PerfilUpdateRequest(
        @Schema(example = "Juan")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @Schema(example = "Pérez")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no debe superar 100 caracteres")
        String apellido,

        @Schema(example = "987654321") @Pattern(regexp = "^\\d{9}$", message = "El teléfono debe tener 9 dígitos")
        String telefono,

        @Schema(type = "string", example = "1992-04-15", description = "Solo se aplica a pacientes")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento,

        @Schema(example = "Consultorio 101", description = "Solo se aplica a médicos")
        @Size(max = 50, message = "El consultorio no debe superar 50 caracteres")
        String consultorio) {}
