package com.codesoftlabs.tusalud.app.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class HceResponses {

    private HceResponses() {}

    public record CierreResponse(
            boolean cerrada,
            UUID atencionId,
            LocalDateTime cerradoEn,
            String cerradoPor,
            String hashCorto,
            boolean firmaValida,
            boolean certificadoDePrueba,
            String advertencia) {}

    public record ContenidoClinico(String diagnostico, String tratamiento, String indicaciones, String observaciones) {}

    public record VersionResponse(
            UUID id,
            int numero,
            String tipo,
            String campo,
            String valorAnterior,
            String valorNuevo,
            String motivo,
            String autor,
            LocalDateTime creadoEn) {}

    public record HistorialAtencionResponse(
            UUID atencionId,
            LocalDateTime fecha,
            boolean cerrada,
            ContenidoClinico original,
            ContenidoClinico vigente,
            List<VersionResponse> versiones,
            CierreResponse cierre) {}

    public record PoliticaConservacionResponse(
            UUID id,
            String tipoRegistro,
            String descripcion,
            int aniosMinimos,
            String referencia,
            boolean porConfirmar,
            boolean depuracionBloqueada,
            LocalDateTime actualizadoEn) {}

    public record DuplicadoResponse(
            UUID pacienteAId,
            String nombreA,
            String dniA,
            String historialA,
            UUID pacienteBId,
            String nombreB,
            String dniB,
            String historialB,
            String criterio) {}

    public record FusionResponse(
            UUID id,
            UUID destinoId,
            String destinoNombre,
            String origenHistorial,
            String origenDni,
            String origenNombre,
            String motivo,
            String resumen,
            String realizadoPor,
            LocalDateTime realizadoEn) {}
}
