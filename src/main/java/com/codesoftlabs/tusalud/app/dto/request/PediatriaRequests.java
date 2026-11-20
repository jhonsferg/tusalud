package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PediatriaRequests {

    private PediatriaRequests() {}

    public record DosisRequest(
            @NotNull UUID pacienteId,
            @NotNull UUID vacunaId,
            @NotNull @Min(1) @Max(10) Integer numeroDosis,
            @NotNull @PastOrPresent LocalDate fechaAplicacion,
            @NotBlank @Size(max = 40) String lote,
            @Size(max = 150) String establecimiento,
            UUID atencionId,
            @Size(max = 300) String observaciones) {}

    public record AnulacionRequest(
            @NotBlank @Size(min = 5, max = 300) String motivo) {}

    public record MedicionRequest(
            @NotNull UUID pacienteId,
            @NotNull @PastOrPresent LocalDate fecha,
            @DecimalMin("0.3") @DecimalMax("200.0") BigDecimal pesoKg,
            @DecimalMin("15.0") @DecimalMax("230.0") BigDecimal tallaCm,
            @DecimalMin("15.0") @DecimalMax("70.0") BigDecimal perimetroCefalicoCm,
            UUID atencionId,
            @Size(max = 300) String observaciones) {}

    public record EsquemaDosisRequest(
            @NotBlank @Size(max = 20) String vacunaCodigo,
            @NotNull @Min(1) @Max(10) Integer numeroDosis,
            @NotBlank @Size(max = 60) String etiqueta,
            @NotNull @Min(0) Integer edadMinimaDias,
            @NotNull @Min(0) Integer edadRecomendadaDias,
            @Min(0) Integer edadMaximaDias,
            @Min(0) Integer intervaloMinimoDias) {}

    public record EsquemaRequest(
            @NotBlank @Size(max = 150) String nombre,
            @NotNull LocalDate vigenteDesde,
            boolean porValidar,
            @Size(max = 250) String fuente,
            @NotEmpty @Size(max = 200) List<@Valid EsquemaDosisRequest> dosis) {}
}
