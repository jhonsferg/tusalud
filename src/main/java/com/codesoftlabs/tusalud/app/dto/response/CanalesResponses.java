package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.AlcanceRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.CategoriaRetencion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoContacto;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.TipoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CanalesResponses {

    private CanalesResponses() {}

    public record ContactoCanalResponse(
            Canal canal,
            String etiqueta,
            String destinoMascara,
            EstadoContacto estado,
            LocalDateTime consentimientoEn,
            LocalDateTime verificadoEn,
            boolean destinoFijo) {}

    public record TipoPreferenciaResponse(
            TipoNotificacion tipo,
            String etiqueta,
            String categoria,
            boolean obligatoria,
            Map<Canal, Boolean> activos) {}

    public record SilencioResponse(boolean activo, LocalTime desde, LocalTime hasta) {}

    public record MisCanalesResponse(
            List<ContactoCanalResponse> canales,
            List<TipoPreferenciaResponse> preferencias,
            SilencioResponse silencio) {}

    public record PlantillaMensajeResponse(
            TipoNotificacion tipo,
            String etiqueta,
            String categoria,
            Canal canal,
            String asunto,
            String cuerpo,
            int version,
            boolean personalizada,
            LocalDateTime actualizadaEn,
            String actualizadaPor,
            String motivo) {}

    public record PlantillasMensajeResponse(List<String> variables, List<PlantillaMensajeResponse> plantillas) {}

    public record EntregaResponse(
            UUID id,
            TipoNotificacion tipo,
            Canal canal,
            String destino,
            EstadoEntrega estado,
            int intentos,
            LocalDateTime proximoIntento,
            String motivo,
            Integer plantillaVersion,
            boolean urgente,
            LocalDateTime creadoEn,
            LocalDateTime enviadoEn,
            LocalDateTime entregadoEn) {}

    public record PaginaEntregasResponse(
            List<EntregaResponse> contenido, int pagina, int tamano, long total, int totalPaginas) {}

    public record ResumenCanalResponse(Canal canal, Map<EstadoEntrega, Long> porEstado, long total) {}

    public record ResumenEntregasResponse(int dias, long total, List<ResumenCanalResponse> canales) {}

    public record MensajeSimuladoResponse(
            String id, Canal canal, String destino, String asunto, String texto, LocalDateTime fecha) {}

    public record ProveedoresResponse(Map<Canal, String> proveedores, boolean workerHabilitado, int limitePorHora) {}

    public record RespaldoEjecucionResponse(
            UUID id,
            TipoEjecucionRespaldo tipo,
            EstadoEjecucionRespaldo estado,
            AlcanceRespaldo alcance,
            CategoriaRetencion categoria,
            LocalDateTime iniciadoEn,
            LocalDateTime finalizadoEn,
            long duracionSeg,
            Long bytesTotal,
            String archivo,
            String checksum,
            Integer migracionesValidadas,
            Integer tablasValidadas,
            Integer objetosAlmacen,
            boolean copiaRemota,
            String detalle) {}

    public record EstadoRespaldosResponse(
            RespaldoEjecucionResponse ultimoRespaldoExitoso,
            RespaldoEjecucionResponse ultimoRespaldoFallido,
            RespaldoEjecucionResponse ultimaPruebaExitosa,
            RespaldoEjecucionResponse ultimaPrueba,
            Long horasDesdeUltimoRespaldo,
            Long diasDesdeUltimaPrueba,
            boolean respaldoVencido,
            boolean pruebaVencida,
            int maxHorasSinRespaldo,
            int maxDiasSinPrueba,
            int rpoObjetivoHoras,
            int rtoObjetivoMinutos,
            String semaforo) {}
}
