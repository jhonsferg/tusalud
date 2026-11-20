package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPieza;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.Superficie;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class OdontologiaRequests {

    private OdontologiaRequests() {}

    public record CambioPiezaRequest(
            @NotNull @Min(11) @Max(85) Integer pieza,
            Superficie superficie,
            @NotNull EstadoPieza estado,
            @Size(max = 300) String observacion) {}

    public record PlanRequest(
            @NotNull UUID pacienteId, @Size(max = 500) String notas) {}

    public record ItemPlanRequest(
            @NotNull UUID procedimientoId,
            @Min(11) @Max(85) Integer pieza,
            Superficie superficie,
            UUID consentimientoId,
            @Size(max = 300) String observacion) {}

    public record ConsentimientoItemRequest(@NotNull UUID consentimientoId) {}

    public record MotivoPlanRequest(
            @NotBlank @Size(min = 5, max = 300) String motivo) {}

    public record SesionRequest(
            @NotEmpty @Size(max = 30) List<UUID> itemIds,
            UUID citaId,
            @Size(max = 500) String notas) {}

    public record PagoPlanRequest(
            @NotNull @DecimalMin("0.01") BigDecimal monto,
            @NotBlank @Size(max = 30) String metodo,
            @Size(max = 60) String referencia) {}

    public record ProcedimientoRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 40) String categoria,
            @NotNull @DecimalMin("0.00") BigDecimal precio,
            boolean requierePieza,
            boolean requiereSuperficie,
            EstadoPieza estadoResultante,
            boolean requiereConsentimiento,
            boolean activo) {}
}
