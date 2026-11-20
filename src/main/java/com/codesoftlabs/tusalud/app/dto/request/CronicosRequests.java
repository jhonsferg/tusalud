package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.EstadoPlan;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.ParametroSeguimiento;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.ProgramaCronico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CronicosRequests {

    private CronicosRequests() {}

    public record MetaRequest(
            @NotNull ParametroSeguimiento parametro,
            @DecimalMin("0") @DecimalMax("100000") BigDecimal valorMin,
            @DecimalMin("0") @DecimalMax("100000") BigDecimal valorMax,
            @NotBlank @Size(max = 15) String unidad) {}

    public record PlanRequest(
            @NotNull UUID pacienteId,
            @NotNull ProgramaCronico programa,
            @Size(max = 10) String diagnosticoCodigo,
            @NotBlank @Size(max = 1000) String objetivos,
            @Size(max = 2000) String intervenciones,
            @Size(max = 1000) String medicacion,
            UUID recetaId,
            LocalDate inicio,
            @Min(1) @Max(365) int frecuenciaControlDias,
            @Min(1) @Max(24) int cantidadControles,
            @NotEmpty @Size(max = 6) List<@Valid MetaRequest> metas) {}

    public record MedicionRequest(
            @NotNull ParametroSeguimiento parametro,
            @NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal valor,
            LocalDateTime medidoEn) {}

    public record CumplirControlRequest(UUID citaId) {}

    public record EstadoPlanRequest(@NotNull EstadoPlan estado) {}
}
