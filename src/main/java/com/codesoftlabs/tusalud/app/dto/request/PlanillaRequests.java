package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.DestinoExtra;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.ModoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.RegimenLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.SistemaPensionario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoMarcacion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class PlanillaRequests {

    private PlanillaRequests() {}

    public record ContratoRequest(
            @NotNull(message = "El empleado es obligatorio") UUID empleadoId,

            @NotNull(message = "El régimen es obligatorio") RegimenLaboral regimen,

            @NotNull(message = "La remuneración es obligatoria")
            @DecimalMin(value = "0.01", message = "La remuneración debe ser mayor a cero")
            @Max(value = 1000000, message = "La remuneración no puede superar 1000000")
            BigDecimal remuneracion,

            Boolean asignacionFamiliar,

            @NotNull(message = "El sistema pensionario es obligatorio")
            SistemaPensionario sistemaPensionario,

            @Size(max = 30, message = "El nombre de la AFP no puede superar 30 caracteres")
            String afpNombre,

            @Min(value = 1, message = "Las horas semanales deben ser al menos 1")
            @Max(value = 60, message = "Las horas semanales no pueden superar 60")
            Integer horasSemanales,

            @Size(max = 60, message = "El banco no puede superar 60 caracteres")
            String banco,

            @Pattern(regexp = "^[0-9-]{0,30}$", message = "La cuenta solo admite dígitos y guiones")
            String cuenta,

            @NotBlank(message = "El cargo es obligatorio")
            @Size(max = 100, message = "El cargo no puede superar 100 caracteres")
            String cargo,

            @NotNull(message = "La fecha de inicio es obligatoria")
            LocalDate fechaInicio,

            LocalDate fechaFin) {}

    public record FinalizarContratoRequest(
            @NotNull(message = "La fecha de fin es obligatoria")
            LocalDate fechaFin) {}

    public record ParametroRequest(
            @NotBlank(message = "El código es obligatorio")
            @Pattern(regexp = "^[A-Z0-9_]{2,40}$", message = "El código solo admite mayúsculas, dígitos y guion bajo")
            String codigo,

            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0", message = "El valor no puede ser negativo")
            @Max(value = 100000000, message = "El valor es demasiado grande")
            BigDecimal valor,

            @NotNull(message = "La vigencia es obligatoria") LocalDate vigenteDesde,

            @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
            String descripcion) {}

    public record ConceptoRequest(
            @NotBlank(message = "El código es obligatorio")
            @Pattern(regexp = "^[A-Z0-9_]{2,30}$", message = "El código solo admite mayúsculas, dígitos y guion bajo")
            String codigo,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
            String nombre,

            @NotNull(message = "El tipo es obligatorio") TipoConcepto tipo,

            @NotNull(message = "El modo es obligatorio") ModoConcepto modo,

            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0", message = "El valor no puede ser negativo")
            @Max(value = 1000000, message = "El valor es demasiado grande")
            BigDecimal valorDefecto,

            Boolean afectoRenta,
            Boolean afectoPension,
            Boolean afectoEssalud,
            Boolean activo) {}

    public record EmpleadoConceptoRequest(
            @NotNull(message = "El concepto es obligatorio") UUID conceptoId,

            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0", message = "El valor no puede ser negativo")
            @Max(value = 1000000, message = "El valor es demasiado grande")
            BigDecimal valor,

            @NotNull(message = "La vigencia es obligatoria") LocalDate vigenteDesde,

            LocalDate vigenteHasta) {}

    public record MarcacionManualRequest(
            @NotNull(message = "El empleado es obligatorio") UUID empleadoId,

            @NotNull(message = "El tipo es obligatorio") TipoMarcacion tipo,

            @NotNull(message = "La fecha y hora son obligatorias")
            LocalDateTime fechaHora,

            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
            String observacion) {}

    public record MarcacionTerminalRequest(
            @NotBlank(message = "El código es obligatorio")
            @Pattern(regexp = "^[A-Za-z0-9]{4,12}$", message = "Código no válido")
            String codigo,

            TipoMarcacion tipo) {}

    public record TerminalRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
            String nombre) {}

    public record ConsolidarRequest(
            @NotNull(message = "La fecha inicial es obligatoria")
            LocalDate desde,

            @NotNull(message = "La fecha final es obligatoria")
            LocalDate hasta,

            UUID empleadoId) {}

    public record AjusteAsistenciaRequest(
            Boolean faltaJustificada,

            @Min(value = 0, message = "Los minutos de tardanza no pueden ser negativos")
            @Max(value = 1440, message = "Los minutos de tardanza no pueden superar 1440")
            Integer minutosTardanza,

            @Min(value = 0, message = "Los minutos extra no pueden ser negativos")
            @Max(value = 1440, message = "Los minutos extra no pueden superar 1440")
            Integer minutosExtra,

            DestinoExtra extraDestino,

            @NotBlank(message = "La justificación es obligatoria")
            @Size(max = 300, message = "La justificación no puede superar 300 caracteres")
            String justificacion) {}

    public record CalcularPlanillaRequest(
            @NotNull(message = "El año es obligatorio")
            @Min(value = 2000, message = "El año no es válido")
            @Max(value = 2100, message = "El año no es válido")
            Integer anio,

            @NotNull(message = "El mes es obligatorio")
            @Min(value = 1, message = "El mes debe estar entre 1 y 12")
            @Max(value = 12, message = "El mes debe estar entre 1 y 12")
            Integer mes) {}

    public record MotivoPlanillaRequest(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String motivo) {}

    public record PagarPlanillaRequest(
            @NotNull(message = "La fecha de pago es obligatoria")
            LocalDate fechaPago,

            @NotBlank(message = "La referencia del pago es obligatoria")
            @Size(max = 60, message = "La referencia no puede superar 60 caracteres")
            String referencia) {}
}
