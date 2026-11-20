package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoEfectivoLote;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoQuirofano;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.MomentoChecklist;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoCiclo;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.RolQuirurgico;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoAnestesia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoEsterilizacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class QuirofanoResponses {

    private QuirofanoResponses() {}

    public record InsumoDisponibleResponse(UUID id, String codigo, String nombre, String unidad, long stock) {}

    public record EquipoQuirofanoResponse(UUID id, String nombre, String serie) {}

    public record QuirofanoResponse(
            UUID id,
            String codigo,
            String nombre,
            String ubicacion,
            EstadoQuirofano estado,
            boolean activo,
            String observaciones,
            List<EquipoQuirofanoResponse> equipos) {}

    public record MiembroEquipoResponse(UUID usuarioId, String nombre, RolQuirurgico rol) {}

    public record CirugiaResumen(
            UUID id,
            String codigo,
            EstadoCirugia estado,
            TipoCirugia tipo,
            UUID quirofanoId,
            String quirofanoNombre,
            LocalDateTime inicio,
            LocalDateTime fin,
            boolean detalleVisible,
            UUID pacienteId,
            String pacienteNombre,
            String procedimiento,
            String solicitanteNombre,
            boolean enMiEquipo) {}

    public record ChecklistResponse(
            MomentoChecklist momento,
            List<String> items,
            String observaciones,
            String completadoPor,
            LocalDateTime completadoEn) {}

    public record InsumoCirugiaResponse(
            UUID id,
            UUID productoId,
            String producto,
            String unidad,
            int cantidad,
            String registradoPor,
            LocalDateTime registradoEn) {}

    public record SetUsadoResponse(
            UUID id,
            UUID loteId,
            String loteCodigo,
            String set,
            String cicloNumero,
            LocalDateTime esterilizadoEn,
            LocalDateTime venceEn,
            String registradoPor,
            LocalDateTime registradoEn) {}

    public record EventoCirugiaResponse(LocalDateTime fecha, String usuario, String accion, String detalle) {}

    public record ConsentimientoCirugia(UUID id, EstadoConsentimiento estado, LocalDateTime firmadoEn) {}

    public record NotaOperatoriaResponse(
            String hallazgos,
            String tecnica,
            String complicaciones,
            Integer sangradoMl,
            String diagnosticoPostoperatorio,
            String indicacionesPostoperatorias,
            LocalDateTime registradaEn,
            String registradaPor) {}

    public record RegistroAnestesicoResponse(
            Integer asa,
            LocalDateTime inicio,
            LocalDateTime fin,
            String observaciones,
            LocalDateTime registradoEn,
            String registradoPor) {}

    public record CirugiaDetalle(
            CirugiaResumen resumen,
            String diagnosticoCodigo,
            String diagnosticoDescripcion,
            TipoAnestesia tipoAnestesia,
            UUID ingresoId,
            String ingresoCodigo,
            String indicacionesPreoperatorias,
            String motivo,
            LocalDateTime inicioReal,
            LocalDateTime finReal,
            ConsentimientoCirugia consentimientoQuirurgico,
            ConsentimientoCirugia consentimientoAnestesico,
            List<MiembroEquipoResponse> equipo,
            List<ChecklistResponse> checklists,
            List<InsumoCirugiaResponse> insumos,
            List<SetUsadoResponse> sets,
            NotaOperatoriaResponse notaOperatoria,
            RegistroAnestesicoResponse registroAnestesico,
            List<EventoCirugiaResponse> eventos,
            List<String> pendientesParaIniciar,
            List<String> pendientesParaFinalizar,
            boolean clinicoVisible) {}

    public record MiCirugiaResponse(
            UUID id,
            String codigo,
            EstadoCirugia estado,
            String procedimiento,
            String quirofano,
            LocalDateTime inicio,
            String cirujano,
            ConsentimientoCirugia consentimientoQuirurgico,
            ConsentimientoCirugia consentimientoAnestesico) {}

    public record EsterilizadorResponse(
            UUID id, String codigo, String nombre, TipoEsterilizacion tipo, boolean activo) {}

    public record SetInstrumentalResponse(
            UUID id, String codigo, String nombre, String descripcion, int vigenciaDias, boolean activo) {}

    public record LoteEsterilResponse(
            UUID id,
            String codigo,
            UUID setId,
            String setNombre,
            UUID cicloId,
            String cicloNumero,
            LocalDateTime esterilizadoEn,
            LocalDateTime venceEn,
            EstadoEfectivoLote estado,
            boolean utilizable) {}

    public record CicloResponse(
            UUID id,
            String numero,
            UUID esterilizadorId,
            String esterilizador,
            LocalDateTime iniciadoEn,
            BigDecimal temperaturaC,
            BigDecimal presionKpa,
            int duracionMin,
            ResultadoIndicador indicadorQuimico,
            ResultadoIndicador indicadorBiologico,
            ResultadoCiclo resultado,
            String responsable,
            String observaciones,
            LocalDateTime cerradoEn,
            List<LoteEsterilResponse> lotes) {}

    public record UsoLoteResponse(
            String cirugiaCodigo, UUID cirugiaId, EstadoCirugia estado, LocalDateTime fecha, String quirofano) {}

    public record TrazabilidadLoteResponse(
            LoteEsterilResponse lote, ResultadoCiclo resultadoCiclo, List<UsoLoteResponse> usos) {}

    public record AlertaEsterilizacionResponse(
            String tipo, String severidad, String descripcion, String referencia, LocalDateTime fecha) {}
}
