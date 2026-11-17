package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.entities.enums.TipoContrato;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ficha laboral de un integrante del personal")
public record EmpleadoRequest(
        @Schema(
                description = "Usuario del personal; solo se usa al crear",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID usuarioId,

        @Schema(example = "Enfermera de emergencia")
        @NotBlank(message = "El cargo es obligatorio")
        @Size(max = 100, message = "El cargo no debe superar 100 caracteres")
        String cargo,

        @Schema(example = "ENFERMERIA") @NotNull(message = "El área es obligatoria")
        AreaLaboral area,

        @Schema(example = "INDEFINIDO") @NotNull(message = "El tipo de contrato es obligatorio")
        TipoContrato tipoContrato,

        @Schema(example = "2024-03-01") @NotNull(message = "La fecha de ingreso es obligatoria")
        LocalDate fechaIngreso,

        @Schema(example = "2026-12-31") LocalDate fechaFinContrato,

        @Schema(example = "ACTIVO") EstadoEmpleado estado,

        @Schema(example = "4800.00")
        @NotNull(message = "El sueldo es obligatorio")
        @DecimalMin(value = "0.00", message = "El sueldo no puede ser negativo")
        @DecimalMax(value = "1000000.00", message = "El sueldo no puede superar 1 000 000")
        BigDecimal sueldo,

        @Schema(example = "Rosa Gutiérrez") @Size(max = 150, message = "El contacto no debe superar 150 caracteres")
        String contactoEmergenciaNombre,

        @Schema(example = "987654321")
        @Pattern(regexp = "^$|^\\d{9}$", message = "El teléfono de emergencia debe tener 9 dígitos")
        String contactoEmergenciaTelefono,

        @Schema(example = "30")
        @Min(value = 0, message = "Los días de vacaciones no pueden ser negativos")
        @Max(value = 60, message = "Los días de vacaciones no pueden superar 60")
        Integer diasVacacionesAnuales) {}
