package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Dispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoDispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoFarmacia;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DispensacionResponse(
        Long id,
        LocalDateTime fecha,
        String comprobante,
        EstadoDispensacion estado,
        Long pacienteId,
        String pacienteNombre,
        String farmaceuticoNombre,
        MetodoPagoFarmacia metodoPago,
        BigDecimal total,
        String observaciones,
        List<DispensacionItemResponse> items) {

    public static final String CLIENTE_GENERAL = "Cliente general";

    public static DispensacionResponse from(Dispensacion dispensacion) {
        return new DispensacionResponse(
                dispensacion.getId(),
                dispensacion.getFecha(),
                dispensacion.getComprobante(),
                dispensacion.getEstado(),
                dispensacion.getPaciente() == null
                        ? null
                        : dispensacion.getPaciente().getId(),
                dispensacion.getPaciente() == null
                        ? CLIENTE_GENERAL
                        : dispensacion.getPaciente().getUsuario().getNombreCompleto(),
                dispensacion.getUsuario().getNombreCompleto(),
                dispensacion.getMetodoPago(),
                dispensacion.getTotal(),
                dispensacion.getObservaciones(),
                dispensacion.getItems().stream()
                        .map(DispensacionItemResponse::from)
                        .toList());
    }
}
