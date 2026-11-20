package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.DestinoExtra;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoAsistencia;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoContrato;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.ModoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.OrigenMarcacion;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.RegimenLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.SistemaPensionario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoMarcacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class PlanillaResponses {

    private PlanillaResponses() {}

    public record ContratoResponse(
            UUID id,
            UUID empleadoId,
            String empleadoNombre,
            AreaLaboral area,
            String cargo,
            RegimenLaboral regimen,
            BigDecimal remuneracion,
            boolean asignacionFamiliar,
            SistemaPensionario sistemaPensionario,
            String afpNombre,
            int horasSemanales,
            String banco,
            String cuentaEnmascarada,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoContrato estado,
            String codigoMarcacion) {}

    public record ParametroResponse(
            UUID id,
            String codigo,
            BigDecimal valor,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta,
            String descripcion,
            boolean porValidar,
            boolean vigente) {}

    public record ConceptoResponse(
            UUID id,
            String codigo,
            String nombre,
            TipoConcepto tipo,
            ModoConcepto modo,
            BigDecimal valorDefecto,
            boolean afectoRenta,
            boolean afectoPension,
            boolean afectoEssalud,
            boolean activo,
            boolean sistema) {}

    public record EmpleadoConceptoResponse(
            UUID id,
            UUID conceptoId,
            String conceptoCodigo,
            String conceptoNombre,
            TipoConcepto tipo,
            ModoConcepto modo,
            BigDecimal valor,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta,
            boolean activo) {}

    public record MarcacionResponse(
            UUID id,
            UUID empleadoId,
            String empleadoNombre,
            TipoMarcacion tipo,
            LocalDateTime fechaHora,
            OrigenMarcacion origen,
            String observacion) {}

    public record MarcacionTerminalResponse(TipoMarcacion tipo, LocalDateTime fechaHora, String empleado) {}

    public record TerminalResponse(UUID id, String nombre, boolean activo, LocalDateTime ultimoUso) {}

    public record TerminalCreadaResponse(UUID id, String nombre, String token) {}

    public record AsistenciaDiaResponse(
            UUID id,
            UUID empleadoId,
            String empleadoNombre,
            LocalDate fecha,
            LocalTime programadaInicio,
            LocalTime programadaFin,
            LocalDateTime entrada,
            LocalDateTime salida,
            int minutosTardanza,
            int minutosTrabajados,
            int minutosExtra,
            EstadoAsistencia estado,
            DestinoExtra extraDestino,
            boolean faltaJustificada,
            String justificacion,
            boolean ajustada) {}

    public record ResumenAsistenciaResponse(
            LocalDate desde,
            LocalDate hasta,
            int diasPresente,
            int diasTardanza,
            int diasFalta,
            int diasFaltaJustificada,
            int diasAusenciaAprobada,
            int minutosTardanza,
            int minutosTrabajados,
            int minutosExtra,
            String codigoMarcacion,
            List<AsistenciaDiaResponse> dias) {}

    public record ConsolidacionResponse(int empleados, int dias, int ajustadasOmitidas) {}

    public record LineaBoletaResponse(
            String codigo, String nombre, TipoConcepto tipo, BigDecimal base, BigDecimal monto) {}

    public record BoletaResumenResponse(
            UUID id,
            String numero,
            UUID empleadoId,
            String empleadoNombre,
            String cargo,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal neto,
            String banco) {}

    public record BoletaResponse(
            UUID id,
            String numero,
            UUID planillaId,
            String planillaCodigo,
            int anio,
            int mes,
            EstadoPlanilla estadoPlanilla,
            UUID empleadoId,
            String empleadoNombre,
            String cargo,
            RegimenLaboral regimen,
            SistemaPensionario sistemaPensionario,
            String banco,
            String cuentaEnmascarada,
            int diasLaborados,
            int diasFalta,
            int diasNoRemunerados,
            int minutosTardanza,
            int minutosExtraPagados,
            BigDecimal remuneracionBasica,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal rentaQuinta,
            BigDecimal pension,
            BigDecimal aporteEmpleador,
            BigDecimal neto,
            BigDecimal provisionGratificacion,
            BigDecimal provisionCts,
            BigDecimal provisionVacaciones,
            String hash,
            List<LineaBoletaResponse> lineas) {}

    public record MiBoletaResumenResponse(
            UUID id,
            String numero,
            String planillaCodigo,
            int anio,
            int mes,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal neto,
            EstadoPlanilla estadoPlanilla) {}

    public record PlanillaResponse(
            UUID id,
            String codigo,
            int anio,
            int mes,
            EstadoPlanilla estado,
            int cantidadBoletas,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal totalNeto,
            BigDecimal totalAporteEmpleador,
            BigDecimal totalRenta,
            BigDecimal totalPension,
            String hash,
            String calculadaPor,
            LocalDateTime calculadaEn,
            String aprobadaPor,
            LocalDateTime aprobadaEn,
            String pagadaPor,
            LocalDateTime pagadaEn,
            LocalDate fechaPago,
            String referenciaPago,
            String cerradaPor,
            LocalDateTime cerradaEn) {}

    public record PlanillaDetalleResponse(PlanillaResponse planilla, List<BoletaResumenResponse> boletas) {}

    public record ArchivoPlanillaResponse(
            String nombre, String contenido, int filas, String formato, boolean porValidar) {}

    public record PlanillaAuditoriaResponse(
            UUID id,
            LocalDateTime fecha,
            String usuario,
            String entidad,
            UUID entidadId,
            String accion,
            String detalle) {}

    public record ResumenPlanillaResponse(
            int empleadosConContrato,
            BigDecimal masaSalarialMensual,
            PlanillaResponse ultimaPlanilla,
            long planillasPendientesAprobacion,
            long marcacionesHoy) {}
}
