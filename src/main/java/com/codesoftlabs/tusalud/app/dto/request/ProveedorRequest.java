package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de un proveedor")
public record ProveedorRequest(
        @Schema(example = "20512345678")
        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
        String ruc,

        @Schema(example = "Droguería Andina S.A.C.")
        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede superar 150 caracteres")
        String razonSocial,

        @Size(max = 100, message = "El contacto no puede superar 100 caracteres")
        String contacto,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        String telefono,

        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,

        Boolean activo,

        @Min(value = 0, message = "La condición de pago no puede ser negativa")
        @Max(value = 365, message = "La condición de pago no puede superar 365 días")
        Integer condicionPagoDias,

        @Min(value = 0, message = "El plazo de entrega no puede ser negativo")
        @Max(value = 365, message = "El plazo de entrega no puede superar 365 días")
        Integer plazoEntregaDias,

        @Size(max = 80, message = "El banco no puede superar 80 caracteres")
        String banco,

        @Size(max = 40, message = "La cuenta no puede superar 40 caracteres")
        String cuentaBancaria,

        @Size(max = 300, message = "Las notas no pueden superar 300 caracteres")
        String notas) {}
