package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.ConceptoMonto;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoLineaExtracto;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoPeriodo;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EventoContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.LadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.NaturalezaCuenta;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoMovimientoTesoreria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ContabilidadResponses {

    private ContabilidadResponses() {}

    public record CuentaContableResponse(
            UUID id,
            String codigo,
            String nombre,
            TipoCuentaContable tipo,
            NaturalezaCuenta naturaleza,
            boolean aceptaMovimientos,
            boolean activa,
            boolean sistema) {}

    public record PeriodoResponse(
            UUID id,
            int anio,
            int mes,
            String etiqueta,
            EstadoPeriodo estado,
            LocalDate inicio,
            LocalDate fin,
            long asientos,
            long borradores,
            LocalDateTime cerradoEn,
            String cerradoPor,
            LocalDateTime reabiertoEn,
            String reabiertoPor,
            String motivoReapertura) {}

    public record LineaAsientoResponse(
            UUID id,
            int posicion,
            UUID cuentaId,
            String cuentaCodigo,
            String cuentaNombre,
            BigDecimal debe,
            BigDecimal haber,
            String glosa) {}

    public record AsientoResumen(
            UUID id,
            String numero,
            LocalDate fecha,
            String periodo,
            String glosa,
            OrigenAsiento origen,
            EstadoAsiento estado,
            BigDecimal totalDebe,
            BigDecimal totalHaber) {}

    public record AsientoResponse(
            UUID id,
            String numero,
            LocalDate fecha,
            UUID periodoId,
            String periodo,
            String glosa,
            OrigenAsiento origen,
            UUID referenciaId,
            EstadoAsiento estado,
            BigDecimal totalDebe,
            BigDecimal totalHaber,
            String creadoPor,
            LocalDateTime creadoEn,
            String contabilizadoPor,
            LocalDateTime contabilizadoEn,
            UUID revierteAId,
            String revierteANumero,
            UUID reversaId,
            String motivoReversa,
            List<LineaAsientoResponse> lineas) {}

    public record PlantillaLineaResponse(
            UUID id,
            int posicion,
            LadoAsiento lado,
            ConceptoMonto concepto,
            UUID cuentaId,
            String cuentaCodigo,
            String cuentaNombre) {}

    public record PlantillaAsientoResponse(
            UUID id, EventoContable evento, String nombre, boolean activa, List<PlantillaLineaResponse> lineas) {}

    public record AsientoPendienteResponse(
            UUID id,
            String evento,
            UUID referenciaId,
            String error,
            int intentos,
            LocalDateTime creadoEn,
            LocalDateTime resueltoEn) {}

    public record CuentaTesoreriaResponse(
            UUID id,
            String nombre,
            TipoCuentaTesoreria tipo,
            String banco,
            String ultimos4,
            String moneda,
            UUID cuentaContableId,
            String cuentaContableCodigo,
            BigDecimal saldoInicial,
            BigDecimal saldo,
            boolean predeterminada,
            boolean activa) {}

    public record MovimientoResponse(
            UUID id,
            UUID cuentaId,
            String cuentaNombre,
            LocalDate fecha,
            TipoMovimientoTesoreria tipo,
            BigDecimal monto,
            String descripcion,
            String referencia,
            OrigenMovimiento origen,
            UUID asientoId,
            boolean conciliado) {}

    public record ExtractoResponse(
            UUID id,
            UUID cuentaId,
            String cuentaNombre,
            String archivo,
            LocalDate desde,
            LocalDate hasta,
            int lineas,
            int duplicadas,
            String importadoPor,
            LocalDateTime importadoEn) {}

    public record ExtractoLineaResponse(
            UUID id,
            LocalDate fecha,
            String descripcion,
            String referencia,
            BigDecimal monto,
            EstadoLineaExtracto estado,
            UUID movimientoId) {}

    public record SugerenciaConciliacion(ExtractoLineaResponse linea, MovimientoResponse movimiento, int puntaje) {}

    public record ResumenConciliacion(
            UUID cuentaId,
            BigDecimal saldoLibros,
            long lineasPendientes,
            BigDecimal netoExtractoPendiente,
            long movimientosSinConciliar,
            BigDecimal netoLibrosPendiente,
            BigDecimal diferencia) {}

    public record FilaMayor(
            LocalDate fecha,
            String asiento,
            UUID asientoId,
            String glosa,
            BigDecimal debe,
            BigDecimal haber,
            BigDecimal saldo) {}

    public record LibroMayorResponse(
            CuentaContableResponse cuenta,
            LocalDate desde,
            LocalDate hasta,
            BigDecimal saldoInicial,
            BigDecimal totalDebe,
            BigDecimal totalHaber,
            BigDecimal saldoFinal,
            List<FilaMayor> movimientos) {}

    public record FilaBalance(
            String codigo,
            String nombre,
            TipoCuentaContable tipo,
            boolean agregada,
            BigDecimal inicialDeudor,
            BigDecimal inicialAcreedor,
            BigDecimal debe,
            BigDecimal haber,
            BigDecimal finalDeudor,
            BigDecimal finalAcreedor) {}

    public record BalanceComprobacionResponse(
            LocalDate desde,
            LocalDate hasta,
            List<FilaBalance> filas,
            BigDecimal totalDebe,
            BigDecimal totalHaber,
            BigDecimal totalDeudor,
            BigDecimal totalAcreedor,
            boolean cuadra) {}

    public record FilaDiario(
            LocalDate fecha,
            String asiento,
            OrigenAsiento origen,
            EstadoAsiento estado,
            String cuentaCodigo,
            String cuentaNombre,
            String glosa,
            BigDecimal debe,
            BigDecimal haber) {}

    public record LineaEstado(String codigo, String nombre, BigDecimal importe) {}

    public record SeccionEstado(String titulo, List<LineaEstado> lineas, BigDecimal total) {}

    public record EstadoSituacionResponse(
            LocalDate al,
            SeccionEstado activo,
            SeccionEstado pasivo,
            SeccionEstado patrimonio,
            BigDecimal totalPasivoPatrimonio,
            boolean cuadra) {}

    public record EstadoResultadosResponse(
            LocalDate desde, LocalDate hasta, SeccionEstado ingresos, SeccionEstado gastos, BigDecimal resultado) {}

    public record SemanaFlujo(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal cobros,
            BigDecimal pagos,
            BigDecimal neto,
            BigDecimal saldoProyectado) {}

    public record FlujoCajaResponse(
            LocalDate desde, int semanas, BigDecimal saldoInicial, List<SemanaFlujo> detalle, BigDecimal saldoFinal) {}

    public record ResumenContabilidad(
            String periodoActual,
            EstadoPeriodo estadoPeriodoActual,
            long asientosDelPeriodo,
            long borradores,
            long pendientesDeContabilizar,
            BigDecimal saldoTesoreria,
            long extractosSinConciliar,
            BigDecimal porPagar,
            BigDecimal porCobrar) {}

    public record ContabilidadAuditoriaResponse(
            UUID id,
            LocalDateTime fecha,
            String usuario,
            String entidad,
            UUID entidadId,
            String accion,
            String detalle) {}

    public record LibroElectronico(String nombreArchivo, String contenido, int registros, String estado) {}
}
