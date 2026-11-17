package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMuestra;
import com.codesoftlabs.tusalud.app.entities.enums.TipoParametro;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Datos de un examen del catálogo de laboratorio")
public record ExamenLabRequest(
        @Schema(example = "HEM-01") @NotBlank(message = "El código es obligatorio") @Size(max = 20)
        String codigo,

        @Schema(example = "Hemograma completo") @NotBlank(message = "El nombre es obligatorio") @Size(max = 150)
        String nombre,

        @Schema(example = "HEMATOLOGIA") @NotNull(message = "La categoría es obligatoria")
        CategoriaExamen categoria,

        @Schema(example = "SANGRE") @NotNull(message = "El tipo de muestra es obligatorio")
        TipoMuestra muestra,

        @Schema(example = "35.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precio,

        @Schema(example = "6")
        @NotNull(message = "El tiempo de entrega es obligatorio")
        @Min(value = 0, message = "El tiempo de entrega no puede ser negativo")
        Integer tiempoEntregaHoras,

        @Schema(example = "Ayuno de 8 horas") @Size(max = 300)
        String indicaciones,

        @Schema(example = "true") Boolean activo,

        @Valid List<ParametroRequest> parametros) {

    @Schema(description = "Parámetro medido por un examen")
    public record ParametroRequest(
            @Schema(example = "Hemoglobina")
            @NotBlank(message = "El nombre del parámetro es obligatorio")
            @Size(max = 100)
            String nombre,

            @Schema(example = "g/dL") @Size(max = 20) String unidad,

            @Schema(example = "NUMERICO") @NotNull(message = "El tipo del parámetro es obligatorio")
            TipoParametro tipo,

            @Schema(example = "12.0") BigDecimal valorMin,

            @Schema(example = "17.5") BigDecimal valorMax,

            @Schema(example = "1") Integer orden) {}
}
