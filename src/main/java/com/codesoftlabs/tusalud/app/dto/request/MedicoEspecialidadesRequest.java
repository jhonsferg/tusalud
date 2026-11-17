package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Especialidades que atiende un médico")
public record MedicoEspecialidadesRequest(
        @Schema(example = "[1, 3]") @NotEmpty(message = "Indique al menos una especialidad")
        List<@NotNull(message = "La especialidad es obligatoria") Long> especialidadIds) {}
