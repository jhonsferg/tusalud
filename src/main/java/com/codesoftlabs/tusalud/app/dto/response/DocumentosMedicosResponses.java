package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoReferencia;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.FirmanteTipo;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.NivelEstablecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.PrioridadReferencia;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.ViaFirma;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DocumentosMedicosResponses {

    private DocumentosMedicosResponses() {}

    public record ReferenciaResponse(
            UUID id,
            UUID destinoId,
            String destinoNombre,
            String especialidad,
            PrioridadReferencia prioridad,
            EstadoReferencia estado,
            String motivoRechazo) {}

    public record DocumentoMedicoResumen(
            UUID id,
            TipoDocumentoMedico tipo,
            String tipoEtiqueta,
            EstadoDocumentoMedico estado,
            String numero,
            UUID pacienteId,
            String pacienteNombre,
            String medicoNombre,
            String medicoColegiatura,
            LocalDateTime creadoEn,
            LocalDateTime emitidoEn,
            LocalDateTime anuladoEn,
            String motivoAnulacion,
            UUID ingresoId,
            UUID atencionId,
            ReferenciaResponse referencia) {}

    public record DocumentoMedicoDetalle(
            DocumentoMedicoResumen resumen,
            Map<String, Object> contenido,
            boolean autorizaDiagnostico,
            String codigoVerificacion,
            UUID documentoOrigenId,
            UUID referenciaId,
            boolean reemplazado,
            List<UUID> contrarreferencias) {}

    public record DocumentoMinimoResponse(
            TipoDocumentoMedico tipo,
            String tipoEtiqueta,
            EstadoDocumentoMedico estado,
            String numero,
            LocalDateTime fecha) {}

    public record EstablecimientoResponse(
            UUID id,
            String codigoIpress,
            String nombre,
            NivelEstablecimiento nivel,
            String distrito,
            String contacto,
            boolean activo) {}

    public record ConsentimientoPlantillaResponse(
            UUID id,
            String codigo,
            String nombre,
            String texto,
            String riesgos,
            String beneficios,
            String alternativas,
            boolean requiereTestigo,
            int vigenciaDias,
            int version,
            boolean activa) {}

    public record ConsentimientoResumen(
            UUID id,
            String plantillaNombre,
            String procedimiento,
            EstadoConsentimiento estado,
            UUID pacienteId,
            String pacienteNombre,
            String medicoNombre,
            boolean requiereTestigo,
            LocalDateTime creadoEn,
            LocalDateTime firmadoEn,
            LocalDateTime venceEn,
            ViaFirma firmadoVia,
            UUID ingresoId,
            UUID atencionId) {}

    public record ConsentimientoDetalle(
            ConsentimientoResumen resumen,
            int plantillaVersion,
            String textoAceptado,
            FirmanteTipo firmanteTipo,
            String firmanteNombre,
            String firmanteDni,
            String firmanteParentesco,
            String testigoNombre,
            String testigoDni,
            String motivoRevocacion,
            String codigoVerificacion,
            LocalDateTime revocadoEn) {}

    public record ResumenIngresoResponse(
            UUID ingresoId, List<DocumentoMinimoResponse> documentos, List<ConsentimientoResumen> consentimientos) {}

    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public record VerificacionPublicaResponse(
            String resultado,
            String tipo,
            LocalDateTime emitidoEn,
            String medico,
            String colegiatura,
            String establecimiento) {}
}
