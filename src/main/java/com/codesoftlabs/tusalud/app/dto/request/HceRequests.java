package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.HceEnums.CampoCorregible;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class HceRequests {

    private HceRequests() {}

    public record AdendaRequest(
            @NotBlank @Size(max = 2000) String texto,
            @NotBlank @Size(min = 5, max = 300) String motivo) {}

    public record CorreccionRequest(
            @NotNull CampoCorregible campo,
            @NotBlank @Size(max = 1000) String valorNuevo,
            @NotBlank @Size(min = 5, max = 300) String motivo) {}

    public record PoliticaConservacionRequest(
            @Min(1) @Max(100) int aniosMinimos,
            @Size(max = 300) String referencia,
            boolean porConfirmar) {}

    public record FusionPacientesRequest(
            @NotNull UUID origenId,
            @NotNull UUID destinoId,
            @NotBlank @Size(min = 5, max = 300) String motivo) {}
}
