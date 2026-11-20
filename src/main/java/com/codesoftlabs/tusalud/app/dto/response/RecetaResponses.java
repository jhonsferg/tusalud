package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaControl;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaEmbarazo;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CompatibilidadLactancia;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoVenta;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadAlerta;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadInteraccion;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.TipoAlertaReceta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class RecetaResponses {

    private RecetaResponses() {}

    public record PrincipioActivoResponse(
            UUID id,
            String dci,
            String claseTerapeutica,
            String grupoAlergia,
            String aliasAlergia,
            BigDecimal dosisMaxAdultoMgDia,
            BigDecimal dosisMaxPediatricaMgKgDia,
            CategoriaEmbarazo categoriaEmbarazo,
            CompatibilidadLactancia lactancia,
            String avisoRenal,
            boolean activo) {}

    public record MedicamentoResponse(
            UUID id,
            UUID principioActivoId,
            String dci,
            String claseTerapeutica,
            String nombreComercial,
            String formaFarmaceutica,
            String concentracion,
            String viaAdministracion,
            String presentacion,
            CategoriaControl categoriaControl,
            boolean requiereReceta,
            EstadoVenta estadoVenta,
            UUID productoId,
            boolean enPetitorio,
            boolean activo,
            String nombre,
            boolean porValidar) {}

    public record InteraccionResponse(
            UUID id,
            UUID principioAId,
            String principioA,
            UUID principioBId,
            String principioB,
            SeveridadInteraccion severidad,
            String descripcion,
            String recomendacion) {}

    public record AlertaResponse(
            String clave,
            TipoAlertaReceta tipo,
            SeveridadAlerta severidad,
            String mensaje,
            String recomendacion,
            String medicamento,
            boolean requiereJustificacion,
            int minimoJustificacion) {}

    public record AlertasResponse(
            List<AlertaResponse> alertas, int pendientesDeJustificar, boolean hayContraindicadas, boolean porValidar) {}

    public record ItemRecetaResponse(
            UUID id,
            int posicion,
            Integer atencionPosicion,
            UUID medicamentoId,
            String medicamento,
            String dosis,
            String frecuencia,
            String duracion,
            String via,
            BigDecimal dosisUnitariaMg,
            Integer vecesPorDia,
            int cantidad,
            int cantidadDispensada,
            int pendiente,
            boolean controlEspecial,
            UUID productoId) {}

    public record AlertaRegistradaResponse(
            String clave,
            TipoAlertaReceta tipo,
            SeveridadAlerta severidad,
            String mensaje,
            String justificacion,
            String justificadaPor,
            LocalDateTime justificadaEn) {}

    public record RecetaResponse(
            UUID id,
            String numero,
            EstadoRecetaElectronica estado,
            boolean vigente,
            LocalDateTime emitidaEn,
            LocalDateTime vigenteHasta,
            UUID atencionId,
            UUID citaId,
            UUID pacienteId,
            String pacienteNombre,
            String pacienteDni,
            String medicoNombre,
            String medicoColegiatura,
            String diagnosticoCodigo,
            String diagnosticoDescripcion,
            String indicaciones,
            boolean controlEspecial,
            String codigoVerificacion,
            String hash,
            String firmaAlgoritmo,
            String certificadoHuella,
            String certificadoSujeto,
            boolean certificadoDePrueba,
            boolean firmaValida,
            LocalDateTime anuladaEn,
            String motivoAnulacion,
            String reemplazaNumero,
            List<ItemRecetaResponse> items,
            List<AlertaRegistradaResponse> alertas) {}

    public record RecetaResumenResponse(
            UUID id,
            String numero,
            EstadoRecetaElectronica estado,
            boolean vigente,
            LocalDateTime emitidaEn,
            LocalDateTime vigenteHasta,
            UUID pacienteId,
            String pacienteNombre,
            String pacienteDni,
            String medicoNombre,
            int medicamentos,
            boolean controlEspecial) {}

    public record RecetaFarmaciaResponse(
            UUID id,
            String numero,
            EstadoRecetaElectronica estado,
            LocalDateTime emitidaEn,
            LocalDateTime vigenteHasta,
            UUID atencionId,
            String pacienteNombre,
            String pacienteDni,
            String medicoNombre,
            String medicoColegiatura,
            boolean controlEspecial,
            List<ItemRecetaResponse> items) {}

    public record ImportacionCatalogoResponse(int creados, int actualizados, List<String> errores) {}

    public record VerificacionFirmaResponse(
            String numero, boolean hashCoincide, boolean firmaValida, String certificadoHuella, boolean dePrueba) {}
}
