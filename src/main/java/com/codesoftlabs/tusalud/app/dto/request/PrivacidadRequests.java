package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.CampoRectificable;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.SeveridadIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoIncidente;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class PrivacidadRequests {

    private PrivacidadRequests() {}

    public record PoliticaRequest(
            @NotBlank @Size(max = 150) String titulo,
            @NotBlank @Size(min = 50, max = 40000) String contenido,
            @NotNull LocalDate vigenteDesde) {}

    public record BancoDatosRequest(
            @NotBlank @Size(max = 30) String codigo,
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 500) String descripcion,
            @NotBlank @Size(max = 300) String finalidadPrincipal,
            @NotBlank @Size(max = 400) String categoriasDatos,
            boolean contieneSensibles,
            @NotBlank @Size(max = 150) String responsable,
            @NotBlank @Size(max = 150) String ubicacion,
            @NotBlank @Size(max = 600) String medidasSeguridad,
            boolean transferenciaTransfronteriza,
            @Size(max = 40) String numeroInscripcion,
            LocalDate fechaInscripcion,
            boolean activo) {}

    public record FinalidadRequest(
            @NotBlank @Size(max = 40) String codigo,
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 500) String descripcion,
            UUID bancoId,
            @NotBlank @Size(max = 250) String baseLegal,
            boolean obligatoria,
            @NotBlank @Size(max = 200) String categoriaDatos,
            @Min(1) Integer plazoRetencionDias,
            boolean activa) {}

    public record MotivoConsentimientoRequest(
            @Size(max = 300) String motivo) {}

    public record SolicitudArcoRequest(
            @NotNull TipoArco tipo,
            @NotBlank @Size(min = 10, max = 1000) String descripcion,
            CampoRectificable campo,
            @Size(max = 200) String valorPropuesto,
            UUID finalidadId) {}

    public record ResolucionArcoRequest(
            @NotNull DecisionArco decision,
            @NotBlank @Size(min = 10, max = 1000) String resolucion) {}

    public enum DecisionArco {
        ATENDER,
        ATENDER_PARCIALMENTE,
        RECHAZAR
    }

    public record PoliticaRetencionRequest(
            @Min(1) @Max(36500) int plazoDias, boolean activa) {}

    public record IncidenteRequest(
            @NotNull TipoIncidente tipo,
            @NotNull SeveridadIncidente severidad,
            @NotBlank @Size(min = 10, max = 1000) String descripcion,
            @NotBlank @Size(max = 400) String datosAfectados,
            @Min(0) int titularesAfectados,
            LocalDateTime detectadoEn,
            @Size(max = 1000) String contencion) {}

    public record IncidenteActualizacionRequest(
            @NotNull EstadoIncidente estado,
            @Size(max = 1000) String contencion,
            @Size(max = 1000) String causaRaiz,
            boolean autoridadNotificada,
            boolean titularesNotificados,
            UUID responsableId) {}

    public record EncargadoRequest(
            @NotBlank @Size(max = 150) String razonSocial,
            @Size(max = 11) String ruc,
            @NotBlank @Size(max = 200) String servicio,
            @NotBlank @Size(max = 300) String categoriasDatos,
            @NotBlank @Size(max = 60) String pais,
            boolean transfronterizo,
            LocalDate contratoVigenteHasta,
            @Size(max = 150) String contacto,
            boolean activo) {}
}
