package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.InformeImagen;
import com.codesoftlabs.tusalud.app.entities.ItemOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.OrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.repositories.ArchivoImagenRepository.Metadatos;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Orden de imagen diagnóstica, con los campos que el rol del usuario puede ver")
public record OrdenImagenResponse(
        UUID id,
        String codigo,
        LocalDateTime fecha,
        EstadoOrdenImagen estado,
        PrioridadOrden prioridad,
        UUID pacienteId,
        String pacienteNombre,
        String dni,
        UUID medicoId,
        String medicoNombre,
        UUID atencionId,
        String indicacionClinica,
        BigDecimal total,
        LocalDateTime programadaPara,
        String sala,
        LocalDateTime realizadaEn,
        String realizadaPor,
        String observacionesToma,
        LocalDateTime informadaEn,
        Boolean hallazgoCritico,
        String motivoCancelacion,
        List<EstudioItem> estudios,
        List<ArchivoImagenResponse> archivos,
        InformeImagenResponse informe) {

    public record EstudioItem(
            UUID id,
            String codigo,
            String nombre,
            ModalidadImagen modalidad,
            String region,
            String preparacion,
            int duracionMinutos) {

        static EstudioItem completo(ItemOrdenImagen item) {
            var estudio = item.getEstudio();
            return new EstudioItem(
                    estudio.getId(),
                    estudio.getCodigo(),
                    estudio.getNombre(),
                    estudio.getModalidad(),
                    estudio.getRegion(),
                    estudio.getPreparacion(),
                    estudio.getDuracionMinutos());
        }
    }

    public static OrdenImagenResponse resumen(OrdenImagen orden) {
        return completa(orden, List.of(), null);
    }

    public static OrdenImagenResponse completa(OrdenImagen orden, List<Metadatos> archivos, InformeImagen informe) {
        return new OrdenImagenResponse(
                orden.getId(),
                orden.getCodigo(),
                orden.getFecha(),
                orden.getEstado(),
                orden.getPrioridad(),
                orden.getPaciente().getId(),
                orden.getPaciente().getUsuario().getNombreCompleto(),
                orden.getPaciente().getDni(),
                orden.getMedico().getId(),
                orden.getMedico().getUsuario().getNombreCompleto(),
                orden.getAtencion() == null ? null : orden.getAtencion().getId(),
                orden.getIndicacionClinica(),
                orden.getTotal(),
                orden.getProgramadaPara(),
                orden.getSala(),
                orden.getRealizadaEn(),
                orden.getRealizadaPor() == null ? null : orden.getRealizadaPor().getNombreCompleto(),
                orden.getObservacionesToma(),
                orden.getInformadaEn(),
                orden.isHallazgoCritico(),
                orden.getMotivoCancelacion(),
                orden.getItems().stream().map(EstudioItem::completo).toList(),
                archivos.stream().map(ArchivoImagenResponse::from).toList(),
                informe == null ? null : InformeImagenResponse.completo(informe));
    }

    public static OrdenImagenResponse paraPaciente(OrdenImagen orden, List<Metadatos> archivos, InformeImagen informe) {
        boolean informada = orden.getEstado() == EstadoOrdenImagen.INFORMADA;
        boolean mostrarPreparacion =
                orden.getEstado() == EstadoOrdenImagen.SOLICITADA || orden.getEstado() == EstadoOrdenImagen.PROGRAMADA;
        List<EstudioItem> estudios = orden.getItems().stream()
                .map(item -> {
                    EstudioItem completo = EstudioItem.completo(item);
                    return new EstudioItem(
                            completo.id(),
                            completo.codigo(),
                            completo.nombre(),
                            completo.modalidad(),
                            completo.region(),
                            mostrarPreparacion ? completo.preparacion() : null,
                            completo.duracionMinutos());
                })
                .toList();
        return new OrdenImagenResponse(
                orden.getId(),
                orden.getCodigo(),
                orden.getFecha(),
                orden.getEstado(),
                orden.getPrioridad(),
                orden.getPaciente().getId(),
                orden.getPaciente().getUsuario().getNombreCompleto(),
                null,
                orden.getMedico().getId(),
                orden.getMedico().getUsuario().getNombreCompleto(),
                null,
                null,
                null,
                orden.getEstado() == EstadoOrdenImagen.PROGRAMADA ? orden.getProgramadaPara() : null,
                orden.getEstado() == EstadoOrdenImagen.PROGRAMADA ? orden.getSala() : null,
                null,
                null,
                null,
                informada ? orden.getInformadaEn() : null,
                null,
                orden.getEstado() == EstadoOrdenImagen.CANCELADA ? orden.getMotivoCancelacion() : null,
                estudios,
                informada ? archivos.stream().map(ArchivoImagenResponse::from).toList() : List.of(),
                informada && informe != null ? InformeImagenResponse.paraPaciente(informe) : null);
    }

    public static OrdenImagenResponse deAdministracion(OrdenImagen orden) {
        return new OrdenImagenResponse(
                orden.getId(),
                orden.getCodigo(),
                orden.getFecha(),
                orden.getEstado(),
                orden.getPrioridad(),
                orden.getPaciente().getId(),
                orden.getPaciente().getUsuario().getNombreCompleto(),
                null,
                null,
                null,
                null,
                null,
                null,
                orden.getProgramadaPara(),
                orden.getSala(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null);
    }
}
