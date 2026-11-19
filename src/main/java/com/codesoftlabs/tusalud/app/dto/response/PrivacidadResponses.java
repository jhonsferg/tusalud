package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.BancoDatos;
import com.codesoftlabs.tusalud.app.entities.EncargadoTratamiento;
import com.codesoftlabs.tusalud.app.entities.FinalidadTratamiento;
import com.codesoftlabs.tusalud.app.entities.IncidenteDatos;
import com.codesoftlabs.tusalud.app.entities.PoliticaPrivacidad;
import com.codesoftlabs.tusalud.app.entities.PoliticaRetencion;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.AccionRetencion;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.SeveridadIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoIncidente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class PrivacidadResponses {

    private PrivacidadResponses() {}

    public record PoliticaResponse(
            UUID id,
            int version,
            String titulo,
            String contenido,
            LocalDate vigenteDesde,
            boolean publicada,
            String hash,
            LocalDateTime creadaEn,
            long aceptaciones) {

        public static PoliticaResponse de(PoliticaPrivacidad p, long aceptaciones) {
            return new PoliticaResponse(
                    p.getId(),
                    p.getVersion(),
                    p.getTitulo(),
                    p.getContenido(),
                    p.getVigenteDesde(),
                    p.isPublicada(),
                    p.getHash(),
                    p.getCreadaEn(),
                    aceptaciones);
        }
    }

    public record PoliticaPublicaResponse(
            int version, String titulo, String contenido, LocalDate vigenteDesde, String hash) {

        public static PoliticaPublicaResponse de(PoliticaPrivacidad p) {
            return new PoliticaPublicaResponse(
                    p.getVersion(), p.getTitulo(), p.getContenido(), p.getVigenteDesde(), p.getHash());
        }
    }

    public record EstadoAceptacionResponse(
            Integer versionVigente,
            UUID politicaId,
            boolean aceptada,
            LocalDateTime aceptadaEn,
            Integer versionAceptada) {}

    public record BancoDatosResponse(
            UUID id,
            String codigo,
            String nombre,
            String descripcion,
            String finalidadPrincipal,
            String categoriasDatos,
            boolean contieneSensibles,
            String responsable,
            String ubicacion,
            String medidasSeguridad,
            boolean transferenciaTransfronteriza,
            String numeroInscripcion,
            LocalDate fechaInscripcion,
            boolean activo,
            LocalDateTime actualizadoEn) {

        public static BancoDatosResponse de(BancoDatos b) {
            return new BancoDatosResponse(
                    b.getId(),
                    b.getCodigo(),
                    b.getNombre(),
                    b.getDescripcion(),
                    b.getFinalidadPrincipal(),
                    b.getCategoriasDatos(),
                    b.isContieneSensibles(),
                    b.getResponsable(),
                    b.getUbicacion(),
                    b.getMedidasSeguridad(),
                    b.isTransferenciaTransfronteriza(),
                    b.getNumeroInscripcion(),
                    b.getFechaInscripcion(),
                    b.isActivo(),
                    b.getActualizadoEn());
        }
    }

    public record FinalidadResponse(
            UUID id,
            String codigo,
            String nombre,
            String descripcion,
            UUID bancoId,
            String bancoNombre,
            String baseLegal,
            boolean obligatoria,
            String categoriaDatos,
            Integer plazoRetencionDias,
            boolean activa) {

        public static FinalidadResponse de(FinalidadTratamiento f) {
            return new FinalidadResponse(
                    f.getId(),
                    f.getCodigo(),
                    f.getNombre(),
                    f.getDescripcion(),
                    f.getBanco() == null ? null : f.getBanco().getId(),
                    f.getBanco() == null ? null : f.getBanco().getNombre(),
                    f.getBaseLegal(),
                    f.isObligatoria(),
                    f.getCategoriaDatos(),
                    f.getPlazoRetencionDias(),
                    f.isActiva());
        }
    }

    public record ConsentimientoEstadoResponse(
            UUID finalidadId,
            String codigo,
            String nombre,
            String descripcion,
            String baseLegal,
            boolean obligatoria,
            String estado,
            LocalDateTime desde) {}

    public record HistorialConsentimientoResponse(
            UUID id,
            String finalidad,
            String accion,
            String origen,
            Integer politicaVersion,
            String motivo,
            LocalDateTime registradoEn) {}

    public record EventoArcoResponse(
            UUID id, String accion, String detalle, String actor, LocalDateTime registradoEn) {}

    public record BloqueoResponse(String categoria, String estado, String motivo, String baseLegal) {}

    public record PreviaCancelacionResponse(List<BloqueoResponse> categorias, boolean hayBloqueos) {}

    public record SolicitudArcoResponse(
            UUID id,
            String codigo,
            TipoArco tipo,
            EstadoArco estado,
            String descripcion,
            String campo,
            String valorPropuesto,
            String solicitante,
            UUID usuarioId,
            String responsable,
            LocalDateTime recibidaEn,
            LocalDate venceEn,
            boolean vencida,
            LocalDateTime resueltaEn,
            String resolucion,
            boolean tieneExportacion) {}

    public record SolicitudArcoDetalle(
            SolicitudArcoResponse solicitud,
            UUID finalidadId,
            String finalidad,
            List<BloqueoResponse> resultado,
            List<EventoArcoResponse> eventos) {}

    public record AccesoDatosResponse(
            UUID id, LocalDateTime fecha, String accion, String detalle, String realizadoPor) {}

    public record PoliticaRetencionResponse(
            UUID id,
            String tipoDato,
            String nombre,
            String descripcion,
            int plazoDias,
            AccionRetencion accion,
            String baseLegal,
            boolean activa,
            LocalDateTime ultimaEjecucion) {

        public static PoliticaRetencionResponse de(PoliticaRetencion p) {
            return new PoliticaRetencionResponse(
                    p.getId(),
                    p.getTipoDato(),
                    p.getNombre(),
                    p.getDescripcion(),
                    p.getPlazoDias(),
                    p.getAccion(),
                    p.getBaseLegal(),
                    p.isActiva(),
                    p.getUltimaEjecucion());
        }
    }

    public record EjecucionRetencionResponse(
            UUID id, String politica, LocalDateTime ejecutadaEn, int afectados, String detalle) {}

    public record IncidenteResponse(
            UUID id,
            String codigo,
            TipoIncidente tipo,
            SeveridadIncidente severidad,
            EstadoIncidente estado,
            String descripcion,
            String datosAfectados,
            int titularesAfectados,
            LocalDateTime detectadoEn,
            LocalDateTime notificarAutoridadAntesDe,
            LocalDateTime autoridadNotificadaEn,
            LocalDateTime titularesNotificadosEn,
            String contencion,
            String causaRaiz,
            LocalDateTime cerradoEn,
            String responsable,
            String registradoPor,
            boolean vencido,
            long horasRestantes) {

        public static IncidenteResponse de(IncidenteDatos i, LocalDateTime ahora) {
            boolean pendiente = i.getAutoridadNotificadaEn() == null && i.getEstado() != EstadoIncidente.CERRADO;
            long horas = java.time.Duration.between(ahora, i.getNotificarAutoridadAntesDe())
                    .toHours();
            return new IncidenteResponse(
                    i.getId(),
                    i.getCodigo(),
                    i.getTipo(),
                    i.getSeveridad(),
                    i.getEstado(),
                    i.getDescripcion(),
                    i.getDatosAfectados(),
                    i.getTitularesAfectados(),
                    i.getDetectadoEn(),
                    i.getNotificarAutoridadAntesDe(),
                    i.getAutoridadNotificadaEn(),
                    i.getTitularesNotificadosEn(),
                    i.getContencion(),
                    i.getCausaRaiz(),
                    i.getCerradoEn(),
                    i.getResponsable() == null ? null : i.getResponsable().getNombreCompleto(),
                    i.getRegistradoPor().getNombreCompleto(),
                    pendiente && horas < 0,
                    pendiente ? horas : 0);
        }
    }

    public record EncargadoResponse(
            UUID id,
            String razonSocial,
            String ruc,
            String servicio,
            String categoriasDatos,
            String pais,
            boolean transfronterizo,
            LocalDate contratoVigenteHasta,
            boolean contratoVencido,
            String contacto,
            boolean activo) {

        public static EncargadoResponse de(EncargadoTratamiento e) {
            return new EncargadoResponse(
                    e.getId(),
                    e.getRazonSocial(),
                    e.getRuc(),
                    e.getServicio(),
                    e.getCategoriasDatos(),
                    e.getPais(),
                    e.isTransfronterizo(),
                    e.getContratoVigenteHasta(),
                    e.getContratoVigenteHasta() != null
                            && e.getContratoVigenteHasta().isBefore(LocalDate.now()),
                    e.getContacto(),
                    e.isActivo());
        }
    }

    public record RegistroTratamientosResponse(
            List<BancoDatosResponse> bancos, List<FinalidadResponse> finalidades, List<EncargadoResponse> encargados) {}

    public record EstadoPrivacidadResponse(
            long solicitudesAbiertas,
            long solicitudesVencidas,
            long incidentesAbiertos,
            long incidentesVencidos,
            Integer versionPolitica,
            LocalDateTime ultimaRetencion) {}
}
