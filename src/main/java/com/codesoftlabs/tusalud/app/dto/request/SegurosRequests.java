package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCobertura;
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

public final class SegurosRequests {

    private SegurosRequests() {}

    public record AseguradoraRequest(
            @NotBlank(message = "El código es obligatorio")
            @Size(max = 20, message = "El código no puede superar 20 caracteres")
            String codigo,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
            String nombre,

            @NotNull(message = "El tipo es obligatorio") TipoAseguradora tipo,

            @Pattern(regexp = "^\\d{11}$", message = "El RUC debe tener 11 dígitos")
            String ruc,

            @Size(max = 150, message = "El contacto no puede superar 150 caracteres")
            String contacto,

            Boolean activa) {}

    public record ReglaRequest(
            @NotNull(message = "La categoría es obligatoria")
            CategoriaServicio categoria,

            @NotNull(message = "El tipo de cobertura es obligatorio")
            TipoCobertura tipo,

            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0.00", message = "El valor no puede ser negativo")
            @DecimalMax(value = "99999.99", message = "El valor es demasiado grande")
            BigDecimal valor,

            @DecimalMin(value = "0.01", message = "El tope por servicio debe ser mayor a cero")
            BigDecimal topePorServicio,

            boolean requierePreautorizacion) {}

    public record ConvenioRequest(
            @NotNull(message = "La aseguradora es obligatoria")
            UUID aseguradoraId,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
            String nombre,

            @NotNull(message = "El inicio de la vigencia es obligatorio")
            LocalDate vigenciaDesde,

            @NotNull(message = "El fin de la vigencia es obligatorio")
            LocalDate vigenciaHasta,

            @DecimalMin(value = "0.01", message = "El tope anual debe ser mayor a cero")
            BigDecimal topeAnual,

            Boolean activo,

            @NotEmpty(message = "Debe definir al menos una regla de cobertura")
            List<@Valid ReglaRequest> reglas,

            List<@Size(max = 40, message = "Cada exclusión admite hasta 40 caracteres") String> exclusiones) {}

    public record PolizaRequest(
            @NotNull(message = "El paciente es obligatorio") UUID pacienteId,

            @NotNull(message = "El convenio es obligatorio") UUID convenioId,

            @NotBlank(message = "El número de póliza es obligatorio")
            @Size(max = 30, message = "El número no puede superar 30 caracteres")
            String numero,

            @NotBlank(message = "El titular es obligatorio")
            @Size(max = 120, message = "El titular no puede superar 120 caracteres")
            String titularNombre,

            @NotBlank(message = "El parentesco es obligatorio")
            @Size(max = 30, message = "El parentesco no puede superar 30 caracteres")
            String parentesco,

            @Size(max = 60, message = "El plan no puede superar 60 caracteres")
            String plan,

            @NotNull(message = "El inicio de la vigencia es obligatorio")
            LocalDate vigenciaDesde,

            @NotNull(message = "El fin de la vigencia es obligatorio")
            LocalDate vigenciaHasta) {}

    public record PolizaEstadoRequest(
            @NotNull(message = "El estado es obligatorio") EstadoPoliza estado,

            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String motivo) {}

    public record PreautorizacionRequest(
            @NotNull(message = "El paciente es obligatorio") UUID pacienteId,

            @NotNull(message = "La categoría es obligatoria")
            CategoriaServicio categoria,

            UUID referenciaId,

            @NotBlank(message = "La descripción es obligatoria")
            @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
            String descripcion,

            @NotNull(message = "El monto estimado es obligatorio")
            @DecimalMin(value = "0.00", message = "El monto estimado no puede ser negativo")
            BigDecimal montoEstimado) {}

    public record AprobarPreautorizacionRequest(
            @NotNull(message = "El monto aprobado es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto aprobado debe ser mayor a cero")
            BigDecimal montoAprobado,

            @Min(value = 1, message = "La vigencia mínima es de 1 día")
            @Max(value = 180, message = "La vigencia máxima es de 180 días")
            Integer vigenciaDias) {}

    public record RechazarPreautorizacionRequest(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
            String motivo) {}

    public record DesgloseRequest(
            @NotNull(message = "El origen del cobro es obligatorio")
            OrigenComprobante origen,

            UUID referenciaId) {}

    public record LiquidacionCrearRequest(
            @NotNull(message = "La aseguradora es obligatoria")
            UUID aseguradoraId,

            @NotNull(message = "La fecha inicial es obligatoria")
            LocalDate desde,

            @NotNull(message = "La fecha final es obligatoria")
            LocalDate hasta) {}

    public record LiquidacionPagoRequest(
            @NotNull(message = "El monto pagado es obligatorio")
            @DecimalMin(value = "0.00", message = "El monto pagado no puede ser negativo")
            BigDecimal montoPagado,

            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String motivoGlosa) {}
}
