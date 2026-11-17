package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Resultados de un examen de una orden de laboratorio")
public record ResultadosLabRequest(
        @NotEmpty(message = "Debe registrar al menos un resultado") @Valid
        List<ResultadoValor> resultados,

        @Schema(example = "Muestra levemente lipémica") @Size(max = 500)
        String observaciones) {

    @Schema(description = "Valor medido para un parámetro")
    public record ResultadoValor(
            @Schema(example = "1") @NotNull(message = "El parámetro es obligatorio")
            Long parametroId,

            @Schema(example = "13.4") @NotBlank(message = "El valor es obligatorio") @Size(max = 100)
            String valor) {}
}
