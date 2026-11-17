package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Datos de un estudio del catálogo de imágenes diagnósticas")
public record EstudioImagenRequest(
        @Schema(example = "RX-TORAX") @NotBlank(message = "El código es obligatorio") @Size(max = 20)
        String codigo,

        @Schema(example = "Radiografía de tórax PA y lateral")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String nombre,

        @Schema(example = "RX") @NotNull(message = "La modalidad es obligatoria")
        ModalidadImagen modalidad,

        @Schema(example = "Tórax") @NotBlank(message = "La región es obligatoria") @Size(max = 60)
        String region,

        @Schema(example = "Retirar objetos metálicos de la zona") @Size(max = 400)
        String preparacion,

        @Schema(example = "80.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precio,

        @Schema(example = "20")
        @NotNull(message = "La duración es obligatoria")
        @Min(value = 5, message = "La duración mínima es de 5 minutos")
        @Max(value = 240, message = "La duración máxima es de 240 minutos")
        Integer duracionMinutos,

        @Schema(example = "true") Boolean activo) {}
