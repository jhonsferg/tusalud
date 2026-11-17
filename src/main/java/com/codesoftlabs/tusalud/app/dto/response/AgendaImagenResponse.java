package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.ItemOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.OrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Cita de sala de un estudio, sin datos clínicos")
public record AgendaImagenResponse(
        UUID id,
        String codigo,
        EstadoOrdenImagen estado,
        PrioridadOrden prioridad,
        LocalDateTime programadaPara,
        int duracionMinutos,
        String sala,
        String pacienteNombre,
        List<ModalidadImagen> modalidades) {

    public static AgendaImagenResponse from(OrdenImagen orden) {
        int duracion = orden.getItems().stream()
                .mapToInt(item -> item.getEstudio().getDuracionMinutos())
                .sum();
        List<ModalidadImagen> modalidades = orden.getItems().stream()
                .map(ItemOrdenImagen::getEstudio)
                .map(estudio -> estudio.getModalidad())
                .distinct()
                .toList();
        return new AgendaImagenResponse(
                orden.getId(),
                orden.getCodigo(),
                orden.getEstado(),
                orden.getPrioridad(),
                orden.getProgramadaPara(),
                Math.max(duracion, 5),
                orden.getSala(),
                orden.getPaciente().getUsuario().getNombreCompleto(),
                modalidades);
    }
}
