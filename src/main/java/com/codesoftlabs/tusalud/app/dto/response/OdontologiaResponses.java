package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoItemPlan;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPieza;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPlan;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.Superficie;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OdontologiaResponses {

    private OdontologiaResponses() {}

    public record ProcedimientoResponse(
            UUID id,
            String codigo,
            String nombre,
            String categoria,
            BigDecimal precio,
            boolean requierePieza,
            boolean requiereSuperficie,
            EstadoPieza estadoResultante,
            boolean requiereConsentimiento,
            boolean activo) {}

    public record PiezaOdontograma(
            int pieza,
            String dentadura,
            EstadoPieza estado,
            String observacion,
            Map<Superficie, EstadoPieza> superficies,
            LocalDateTime ultimoCambio) {}

    public record OdontogramaResponse(
            UUID pacienteId, String pacienteNombre, Integer edadAnios, List<PiezaOdontograma> piezas) {}

    public record CambioOdontogramaResponse(
            UUID id,
            int pieza,
            Superficie superficie,
            EstadoPieza estado,
            String observacion,
            String registradoPor,
            LocalDateTime registradoEn,
            UUID itemId) {}

    public record ItemPlanResponse(
            UUID id,
            UUID procedimientoId,
            String codigo,
            String nombre,
            Integer pieza,
            Superficie superficie,
            BigDecimal precio,
            EstadoItemPlan estado,
            boolean requiereConsentimiento,
            UUID consentimientoId,
            LocalDateTime realizadoEn,
            String observacion) {}

    public record SesionOdontologicaResponse(
            UUID id, LocalDateTime fecha, String medicoNombre, UUID citaId, String notas, List<UUID> itemIds) {}

    public record PagoPlanResponse(
            UUID id,
            BigDecimal monto,
            String metodo,
            String referencia,
            String registradoPor,
            LocalDateTime registradoEn) {}

    public record PlanResumen(
            UUID id,
            UUID pacienteId,
            String pacienteNombre,
            String medicoNombre,
            EstadoPlan estado,
            BigDecimal total,
            BigDecimal pagado,
            BigDecimal saldo,
            LocalDateTime creadoEn) {}

    public record PlanResponse(
            UUID id,
            UUID pacienteId,
            String pacienteNombre,
            String medicoNombre,
            EstadoPlan estado,
            String notas,
            BigDecimal total,
            BigDecimal pagado,
            BigDecimal saldo,
            LocalDateTime creadoEn,
            LocalDateTime presentadoEn,
            LocalDateTime resueltoEn,
            String motivoRechazo,
            LocalDateTime completadoEn,
            List<ItemPlanResponse> items,
            List<SesionOdontologicaResponse> sesiones,
            List<PagoPlanResponse> pagos) {}
}
