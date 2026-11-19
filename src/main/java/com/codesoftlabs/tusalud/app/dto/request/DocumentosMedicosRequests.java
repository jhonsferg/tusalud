package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoReferencia;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.FirmanteTipo;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.NivelEstablecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.ViaFirma;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public final class DocumentosMedicosRequests {

    private DocumentosMedicosRequests() {}

    public record DocumentoMedicoRequest(
            @NotNull UUID pacienteId,
            @NotNull TipoDocumentoMedico tipo,
            UUID atencionId,
            UUID ingresoId,
            UUID documentoOrigenId,
            UUID referenciaId,
            boolean autorizaDiagnostico,
            @NotNull Map<String, Object> contenido) {}

    public record ActualizarDocumentoRequest(
            boolean autorizaDiagnostico, @NotNull Map<String, Object> contenido) {}

    public record MotivoRequest(
            @NotBlank @Size(min = 5, max = 300) String motivo) {}

    public record EpicrisisBorradorRequest(@NotNull UUID ingresoId) {}

    public record EstablecimientoRequest(
            @Size(max = 20) String codigoIpress,
            @NotBlank @Size(max = 150) String nombre,
            @NotNull NivelEstablecimiento nivel,
            @Size(max = 100) String distrito,
            @Size(max = 150) String contacto,
            boolean activo) {}

    public record ReferenciaEstadoRequest(
            @NotNull EstadoReferencia estado,
            @Size(max = 300) String motivoRechazo) {}

    public record ConsentimientoPlantillaRequest(
            @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Za-z0-9_-]+$")
            String codigo,

            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 4000) String texto,
            @NotBlank @Size(max = 4000) String riesgos,
            @NotBlank @Size(max = 4000) String beneficios,
            @NotBlank @Size(max = 4000) String alternativas,
            boolean requiereTestigo,
            @Min(1) @Max(3650) int vigenciaDias,
            boolean activa) {}

    public record ConsentimientoSolicitudRequest(
            @NotNull UUID plantillaId,
            @NotNull UUID pacienteId,
            UUID atencionId,
            UUID ingresoId,
            UUID ordenImagenId,
            @NotBlank @Size(max = 200) String procedimiento) {}

    public record FirmaRequest(
            @NotBlank @Size(max = 700_000) String firmaPng,
            @NotNull FirmanteTipo firmanteTipo,
            @NotBlank @Size(max = 150) String nombre,

            @NotBlank @Pattern(regexp = "^[0-9A-Za-z]{8,12}$")
            String dni,

            @Size(max = 50) String parentesco,
            @Size(max = 150) String testigoNombre,
            @Pattern(regexp = "^([0-9A-Za-z]{8,12})?$") String testigoDni,
            @NotNull ViaFirma via) {}
}
