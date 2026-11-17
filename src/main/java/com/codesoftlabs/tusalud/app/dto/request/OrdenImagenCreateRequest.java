package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Solicitud de un estudio de imagen diagnóstica")
public record OrdenImagenCreateRequest(
        @NotNull(message = "El paciente es obligatorio") UUID pacienteId,

        @Schema(description = "Atención de la que nace la orden, opcional")
        UUID atencionId,

        @Schema(example = "NORMAL") @NotNull(message = "La prioridad es obligatoria")
        PrioridadOrden prioridad,

        @Schema(example = "Tos persistente de tres semanas, descartar neumonía")
        @NotBlank(message = "La indicación clínica es obligatoria")
        @Size(max = 500)
        String indicacionClinica,

        @NotEmpty(message = "Debe elegir al menos un estudio")
        @Size(max = 10, message = "Una orden admite hasta 10 estudios")
        List<@NotNull(message = "El estudio es obligatorio") UUID> estudioIds) {}
