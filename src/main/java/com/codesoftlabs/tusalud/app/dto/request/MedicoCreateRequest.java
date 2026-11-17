package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Alta de un médico (crea también su usuario con ROLE_MEDICO)")
public record MedicoCreateRequest(
        @Schema(example = "Ana")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @Schema(example = "Torres")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no debe superar 100 caracteres")
        String apellido,

        @Schema(example = "ana.torres@tusalud.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no debe superar 150 caracteres")
        String email,

        @Schema(example = "Medico123!")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Schema(example = "912345678") @Pattern(regexp = "^\\d{9}$", message = "El teléfono debe tener 9 dígitos")
        String telefono,

        @Schema(example = "CMP-54321")
        @NotBlank(message = "La colegiatura (CMP) es obligatoria")
        @Size(max = 20, message = "La colegiatura no debe superar 20 caracteres")
        String colegiatura,

        @Schema(example = "Consultorio 204") @Size(max = 50, message = "El consultorio no debe superar 50 caracteres")
        String consultorio,

        @Schema(example = "[1, 3]") @NotEmpty(message = "Indique al menos una especialidad")
        List<@NotNull(message = "La especialidad es obligatoria") Long> especialidadIds) {}
