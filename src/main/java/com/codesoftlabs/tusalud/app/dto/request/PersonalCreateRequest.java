package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Alta de personal de la institución")
public record PersonalCreateRequest(
        @Schema(example = "Laura")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @Schema(example = "Quispe")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no debe superar 100 caracteres")
        String apellido,

        @Schema(example = "lab@tusalud.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no debe superar 150 caracteres")
        String email,

        @Schema(example = "Personal123!")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Schema(example = "901112233") @Pattern(regexp = "^\\d{9}$", message = "El teléfono debe tener 9 dígitos")
        String telefono,

        @Schema(example = "[\"ROLE_LABORATORIO\"]") @NotEmpty(message = "Indique al menos un rol")
        List<@NotNull(message = "El rol es obligatorio") RolNombre> roles) {
    @Override
    public String toString() {
        return "PersonalCreateRequest[REDACTADO]";
    }
}
