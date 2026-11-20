package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoQuirofano;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.MomentoChecklist;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.RolQuirurgico;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoAnestesia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoEsterilizacion;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class QuirofanoRequests {

    private QuirofanoRequests() {}

    public record EquipoQuirofanoRequest(
            @NotBlank @Size(max = 100) String nombre,
            @Size(max = 60) String serie) {}

    public record QuirofanoRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 100) String nombre,
            @Size(max = 150) String ubicacion,
            @NotNull EstadoQuirofano estado,
            Boolean activo,
            @Size(max = 300) String observaciones,
            @Valid @Size(max = 30) List<EquipoQuirofanoRequest> equipos) {}

    public record SolicitudCirugiaRequest(
            @NotNull UUID pacienteId,
            @NotBlank @Size(max = 8) String diagnosticoCodigo,
            @NotBlank @Size(max = 200) String procedimiento,
            @NotNull TipoCirugia tipo,
            @NotNull TipoAnestesia tipoAnestesia,
            UUID ingresoId,
            @Size(max = 500) String indicacionesPreoperatorias) {}

    public record MiembroEquipoRequest(
            @NotNull UUID usuarioId, @NotNull RolQuirurgico rol) {}

    public record ProgramacionRequest(
            @NotNull UUID quirofanoId,
            @NotNull LocalDateTime inicio,
            @NotNull @Min(15) @Max(1440) Integer duracionMin,
            @NotEmpty @Valid @Size(max = 15) List<MiembroEquipoRequest> equipo) {}

    public record MotivoCirugiaRequest(
            @NotBlank @Size(max = 300) String motivo) {}

    public record ChecklistRequest(
            @NotNull MomentoChecklist momento,
            @NotNull @Size(max = 20) List<String> items,
            @Size(max = 300) String observaciones) {}

    public record NotaOperatoriaRequest(
            @NotBlank @Size(max = 4000) String hallazgos,
            @NotBlank @Size(max = 4000) String tecnica,
            @Size(max = 2000) String complicaciones,
            @Min(0) @Max(20000) Integer sangradoMl,
            @NotBlank @Size(max = 300) String diagnosticoPostoperatorio,
            @Size(max = 500) String indicacionesPostoperatorias) {}

    public record RegistroAnestesicoRequest(
            @NotNull @Min(1) @Max(6) Integer asa,
            @NotNull LocalDateTime inicio,
            @NotNull LocalDateTime fin,
            @Size(max = 4000) String observaciones) {}

    public record InsumoCirugiaRequest(
            @NotNull UUID productoId,
            @NotNull @Min(1) @Max(1000) Integer cantidad) {}

    public record UsoSetRequest(@NotNull UUID loteId) {}

    public record EsterilizadorRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 100) String nombre,
            @NotNull TipoEsterilizacion tipo,
            Boolean activo) {}

    public record SetInstrumentalRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 300) String descripcion,
            @NotNull @Min(1) @Max(365) Integer vigenciaDias,
            Boolean activo) {}

    public record SetDelCicloRequest(
            @NotNull UUID setId, @NotNull @Min(1) @Max(50) Integer cantidad) {}

    public record CicloRequest(
            @NotNull UUID esterilizadorId,
            @NotNull @PastOrPresent LocalDateTime iniciadoEn,

            @NotNull @DecimalMin("40.0") @DecimalMax("200.0")
            BigDecimal temperaturaC,

            @DecimalMin("0.0") @DecimalMax("1000.0") BigDecimal presionKpa,
            @NotNull @Min(1) @Max(600) Integer duracionMin,
            @Size(max = 300) String observaciones,
            @NotEmpty @Valid @Size(max = 20) List<SetDelCicloRequest> sets) {}

    public record IndicadoresRequest(
            @NotNull ResultadoIndicador quimico,
            @NotNull ResultadoIndicador biologico,
            @Size(max = 300) String observaciones) {}
}
