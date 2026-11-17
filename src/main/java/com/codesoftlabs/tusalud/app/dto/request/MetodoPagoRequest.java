package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.TipoMetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Tarjeta para pagos (solo se guardan los últimos 4 dígitos; el CVV nunca se almacena)")
public record MetodoPagoRequest(
        @Schema(example = "TARJETA_CREDITO") @NotNull(message = "El tipo de método de pago es obligatorio")
        TipoMetodoPago tipo,

        @Schema(example = "JUAN PEREZ")
        @NotBlank(message = "El titular es obligatorio")
        @Size(max = 100, message = "El titular no debe superar 100 caracteres")
        String titular,

        @Schema(example = "4111 1111 1111 1111")
        @NotBlank(message = "El número de tarjeta es obligatorio")
        @Pattern(
                regexp = "^[0-9 -]+$",
                message = "El número de tarjeta solo puede contener dígitos, espacios o guiones")
        String numeroTarjeta,

        @Schema(example = "12")
        @NotNull(message = "El mes de vencimiento es obligatorio")
        @Min(value = 1, message = "El mes de vencimiento debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes de vencimiento debe estar entre 1 y 12")
        Integer vencimientoMes,

        @Schema(example = "2030")
        @NotNull(message = "El año de vencimiento es obligatorio")
        @Min(value = 2000, message = "El año de vencimiento no es válido")
        @Max(value = 2100, message = "El año de vencimiento no es válido")
        Integer vencimientoAnio,

        @Schema(example = "123", description = "Solo se valida; no se guarda")
        @NotBlank(message = "El CVV es obligatorio")
        @Pattern(regexp = "^\\d{3,4}$", message = "El CVV debe tener 3 o 4 dígitos")
        String cvv,

        @Schema(example = "true") Boolean predeterminado) {}
