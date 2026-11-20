package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoMovimientoTesoreria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ContabilidadRequests {

    private ContabilidadRequests() {}

    public record CuentaContableRequest(
            @NotBlank @Pattern(regexp = "^[0-9]{2,10}$", message = "El código debe tener entre 2 y 10 dígitos")
            String codigo,

            @NotBlank @Size(max = 120) String nombre,
            @NotNull TipoCuentaContable tipo,
            boolean aceptaMovimientos) {}

    public record ActualizarCuentaContableRequest(
            @NotBlank @Size(max = 120) String nombre, boolean aceptaMovimientos, boolean activa) {}

    public record LineaAsientoRequest(
            @NotNull UUID cuentaId,

            @DecimalMin("0.00") @DecimalMax("999999999999.99")
            BigDecimal debe,

            @DecimalMin("0.00") @DecimalMax("999999999999.99")
            BigDecimal haber,

            @Size(max = 200) String glosa) {}

    public record AsientoRequest(
            @NotNull LocalDate fecha,
            @NotBlank @Size(max = 200) String glosa,
            @NotEmpty @Size(min = 2, max = 60) List<@Valid LineaAsientoRequest> lineas) {}

    public record MotivoContableRequest(
            @NotBlank @Size(min = 5, max = 300) String motivo) {}

    public record PeriodoRequest(
            @Min(2000) @Max(2100) int anio, @Min(1) @Max(12) int mes) {}

    public record CuentaLineaPlantillaRequest(
            @NotNull UUID lineaId, @NotNull UUID cuentaId) {}

    public record PlantillaAsientoRequest(
            @NotBlank @Size(max = 120) String nombre,
            boolean activa,
            @NotEmpty List<@Valid CuentaLineaPlantillaRequest> cuentas) {}

    public record CuentaTesoreriaRequest(
            @NotBlank @Size(max = 100) String nombre,
            @NotNull TipoCuentaTesoreria tipo,
            @Size(max = 60) String banco,

            @Pattern(regexp = "^[0-9]{4}$", message = "Indique solo los últimos 4 dígitos")
            String ultimos4,

            @NotNull UUID cuentaContableId,

            @NotNull @DecimalMin("0.00") @DecimalMax("999999999999.99")
            BigDecimal saldoInicial,

            boolean predeterminada,
            Boolean activa) {}

    public record MovimientoTesoreriaRequest(
            @NotNull UUID cuentaId,
            @NotNull LocalDate fecha,
            @NotNull TipoMovimientoTesoreria tipo,

            @NotNull @DecimalMin("0.01") @DecimalMax("999999999999.99")
            BigDecimal monto,

            @NotBlank @Size(max = 200) String descripcion,
            @Size(max = 60) String referencia,
            @NotNull UUID contrapartidaId) {}

    public record ImportarExtractoRequest(
            @NotNull UUID cuentaId,
            @NotBlank @Size(max = 120) String archivo,
            @NotBlank @Size(max = 1_000_000) String contenido) {}

    public record ConciliarRequest(
            @NotNull UUID lineaId, @NotNull UUID movimientoId) {}
}
