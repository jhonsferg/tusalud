package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoFarmacia;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaControl;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaEmbarazo;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CompatibilidadLactancia;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoVenta;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadInteraccion;
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
import java.util.List;
import java.util.UUID;

public final class RecetaRequests {

    private RecetaRequests() {}

    public record PrincipioActivoRequest(
            @NotBlank(message = "La denominación común es obligatoria")
            @Size(max = 120, message = "La denominación común no debe superar 120 caracteres")
            String dci,

            @Size(max = 80, message = "La clase terapéutica no debe superar 80 caracteres")
            String claseTerapeutica,

            @Size(max = 40, message = "El grupo de alergia no debe superar 40 caracteres")
            String grupoAlergia,

            @Size(max = 250, message = "Los alias de alergia no deben superar 250 caracteres")
            String aliasAlergia,

            @DecimalMin(value = "0", message = "La dosis máxima no es válida")
            @DecimalMax(value = "100000", message = "La dosis máxima no es válida")
            BigDecimal dosisMaxAdultoMgDia,

            @DecimalMin(value = "0", message = "La dosis máxima pediátrica no es válida")
            @DecimalMax(value = "1000", message = "La dosis máxima pediátrica no es válida")
            BigDecimal dosisMaxPediatricaMgKgDia,

            CategoriaEmbarazo categoriaEmbarazo,
            CompatibilidadLactancia lactancia,

            @Size(max = 300, message = "El aviso renal no debe superar 300 caracteres")
            String avisoRenal) {}

    public record MedicamentoRequest(
            @NotNull(message = "El principio activo es obligatorio")
            UUID principioActivoId,

            @Size(max = 120, message = "El nombre comercial no debe superar 120 caracteres")
            String nombreComercial,

            @NotBlank(message = "La forma farmacéutica es obligatoria")
            @Size(max = 60, message = "La forma farmacéutica no debe superar 60 caracteres")
            String formaFarmaceutica,

            @NotBlank(message = "La concentración es obligatoria")
            @Size(max = 60, message = "La concentración no debe superar 60 caracteres")
            String concentracion,

            @NotBlank(message = "La vía de administración es obligatoria")
            @Size(max = 30, message = "La vía no debe superar 30 caracteres")
            String viaAdministracion,

            @Size(max = 120, message = "La presentación no debe superar 120 caracteres")
            String presentacion,

            CategoriaControl categoriaControl,
            Boolean requiereReceta,
            EstadoVenta estadoVenta,
            UUID productoId,
            Boolean enPetitorio,
            Boolean activo) {}

    public record InteraccionRequest(
            @NotNull(message = "El primer principio activo es obligatorio")
            UUID principioAId,

            @NotNull(message = "El segundo principio activo es obligatorio")
            UUID principioBId,

            @NotNull(message = "La severidad es obligatoria")
            SeveridadInteraccion severidad,

            @NotBlank(message = "La descripción es obligatoria")
            @Size(max = 400, message = "La descripción no debe superar 400 caracteres")
            String descripcion,

            @Size(max = 400, message = "La recomendación no debe superar 400 caracteres")
            String recomendacion) {}

    public record ItemPrescripcion(
            @NotNull(message = "El medicamento es obligatorio")
            UUID medicamentoId,

            @DecimalMin(value = "0.01", message = "La dosis unitaria no es válida")
            @DecimalMax(value = "100000", message = "La dosis unitaria no es válida")
            BigDecimal dosisUnitariaMg,

            @Min(value = 1, message = "Las veces por día no son válidas")
            @Max(value = 24, message = "Las veces por día no son válidas")
            Integer vecesPorDia) {}

    public record AlertasRequest(
            @NotNull(message = "El paciente es obligatorio") UUID pacienteId,

            @NotEmpty(message = "Indique al menos un medicamento")
            @Size(max = 20, message = "Una receta admite hasta 20 medicamentos")
            @Valid
            List<ItemPrescripcion> items,

            Boolean embarazada,
            Boolean lactando) {}

    public record ItemEmision(
            @NotNull(message = "La posición de la receta de la atención es obligatoria")
            @Min(value = 0, message = "La posición no es válida")
            Integer atencionPosicion,

            @NotNull(message = "El medicamento del catálogo es obligatorio")
            UUID medicamentoId,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            @Max(value = 1000, message = "La cantidad no es válida")
            Integer cantidad,

            @Size(max = 30, message = "La vía no debe superar 30 caracteres")
            String via,

            @DecimalMin(value = "0.01", message = "La dosis unitaria no es válida")
            @DecimalMax(value = "100000", message = "La dosis unitaria no es válida")
            BigDecimal dosisUnitariaMg,

            @Min(value = 1, message = "Las veces por día no son válidas")
            @Max(value = 24, message = "Las veces por día no son válidas")
            Integer vecesPorDia) {}

    public record JustificacionAlerta(
            @NotBlank(message = "La clave de la alerta es obligatoria")
            String clave,

            @NotBlank(message = "La justificación es obligatoria")
            @Size(max = 500, message = "La justificación no debe superar 500 caracteres")
            String texto) {}

    public record EmitirRecetaRequest(
            @NotNull(message = "La atención es obligatoria") UUID atencionId,

            @Min(value = 1, message = "La vigencia debe ser de al menos un día")
            @Max(value = 90, message = "La vigencia no puede superar 90 días")
            Integer vigenciaDias,

            @Size(max = 1000, message = "Las indicaciones no deben superar 1000 caracteres")
            String indicaciones,

            Boolean embarazada,
            Boolean lactando,
            UUID reemplazaId,

            @NotEmpty(message = "Indique al menos un medicamento")
            @Size(max = 20, message = "Una receta admite hasta 20 medicamentos")
            @Valid
            List<ItemEmision> items,

            @Valid List<JustificacionAlerta> justificaciones) {}

    public record AnularRecetaRequest(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
            String motivo) {}

    public record ItemDispensarReceta(
            @NotNull(message = "El ítem es obligatorio") UUID itemId,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer cantidad) {}

    public record DispensarRecetaRequest(
            @NotEmpty(message = "Indique al menos un ítem") @Valid
            List<ItemDispensarReceta> items,

            @NotNull(message = "El método de pago es obligatorio")
            MetodoPagoFarmacia metodoPago,

            @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
            String observaciones) {}
}
