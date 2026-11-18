package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoLiquidacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCobertura;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SegurosResponses {

    private SegurosResponses() {}

    public record AseguradoraResponse(
            UUID id, String codigo, String nombre, TipoAseguradora tipo, String ruc, String contacto, boolean activa) {}

    public record ReglaResponse(
            CategoriaServicio categoria,
            TipoCobertura tipo,
            BigDecimal valor,
            BigDecimal topePorServicio,
            boolean requierePreautorizacion) {}

    public record ConvenioResponse(
            UUID id,
            UUID aseguradoraId,
            String aseguradoraCodigo,
            String aseguradoraNombre,
            String nombre,
            LocalDate vigenciaDesde,
            LocalDate vigenciaHasta,
            BigDecimal topeAnual,
            boolean activo,
            boolean vigente,
            List<ReglaResponse> reglas,
            List<String> exclusiones) {}

    public record PolizaResponse(
            UUID id,
            UUID pacienteId,
            String pacienteNombre,
            String pacienteDni,
            String aseguradoraNombre,
            UUID convenioId,
            String convenioNombre,
            String numero,
            String titularNombre,
            String parentesco,
            String plan,
            LocalDate vigenciaDesde,
            LocalDate vigenciaHasta,
            EstadoPoliza estado,
            Long diasParaVencer,
            String motivoSuspension) {}

    public record MiPolizaResponse(
            UUID id,
            String aseguradoraNombre,
            String convenioNombre,
            String numero,
            String titularNombre,
            String parentesco,
            String plan,
            LocalDate vigenciaDesde,
            LocalDate vigenciaHasta,
            EstadoPoliza estado,
            Long diasParaVencer,
            List<ReglaResponse> coberturas) {}

    public record VerificacionResponse(
            UUID polizaId,
            boolean vigente,
            String estado,
            BigDecimal saldoTope,
            String codigo,
            LocalDateTime verificadaEn,
            String mensaje) {}

    public record CoberturaPacienteResponse(
            UUID pacienteId,
            boolean tieneCobertura,
            String aseguradora,
            EstadoPoliza estado,
            List<CategoriaServicio> requierenPreautorizacion) {}

    public record PreautorizacionResponse(
            UUID id,
            String codigo,
            UUID pacienteId,
            String pacienteNombre,
            String aseguradoraNombre,
            String polizaNumero,
            CategoriaServicio categoria,
            UUID referenciaId,
            String descripcion,
            BigDecimal montoEstimado,
            EstadoPreautorizacion estado,
            String solicitadaPorNombre,
            LocalDateTime solicitadaEn,
            String gestionadaPorNombre,
            LocalDateTime gestionadaEn,
            String codigoAutorizacion,
            BigDecimal montoAprobado,
            LocalDate vigenteHasta,
            String motivoRechazo) {}

    public record LineaDesglose(
            String descripcion, BigDecimal monto, BigDecimal cubierto, BigDecimal copago, String motivo) {}

    public record DesgloseResponse(
            OrigenComprobante origen,
            UUID referenciaId,
            BigDecimal total,
            boolean tieneCobertura,
            String aseguradora,
            String convenio,
            String polizaNumero,
            BigDecimal montoCubierto,
            BigDecimal copago,
            BigDecimal saldoTopeAnual,
            String mensaje,
            List<LineaDesglose> lineas) {}

    public record CuentaAseguradoraResponse(
            UUID id,
            UUID aseguradoraId,
            String aseguradoraNombre,
            String comprobanteCodigo,
            CategoriaServicio categoria,
            String descripcion,
            BigDecimal montoTotal,
            BigDecimal montoCubierto,
            BigDecimal copago,
            LocalDateTime fecha,
            EstadoCuentaAseguradora estado,
            long antiguedadDias,
            String liquidacionCodigo) {}

    public record CarteraResponse(
            UUID aseguradoraId,
            String aseguradoraNombre,
            BigDecimal pendiente,
            long cuentas,
            BigDecimal tramo0a30,
            BigDecimal tramo31a60,
            BigDecimal tramo61a90,
            BigDecimal tramoMas90) {}

    public record LiquidacionResponse(
            UUID id,
            String codigo,
            UUID aseguradoraId,
            String aseguradoraNombre,
            LocalDate desde,
            LocalDate hasta,
            BigDecimal total,
            EstadoLiquidacion estado,
            int cantidadCuentas,
            String creadaPorNombre,
            LocalDateTime creadaEn,
            LocalDateTime enviadaEn,
            LocalDateTime pagadaEn,
            BigDecimal montoPagado,
            BigDecimal montoGlosa,
            String motivoGlosa) {}

    public record SegurosResumenResponse(
            BigDecimal carteraPendiente,
            Long cuentasPendientes,
            Long liquidacionesAbiertas,
            Long polizasPorVencer,
            Long preautorizacionesPendientes) {}
}
