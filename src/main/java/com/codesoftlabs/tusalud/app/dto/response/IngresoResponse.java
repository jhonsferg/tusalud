package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Ingreso;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record IngresoResponse(
        Long id,
        LocalDateTime fecha,
        String documento,
        String proveedorNombre,
        String observaciones,
        BigDecimal total,
        String usuarioNombre,
        List<LoteResponse> items) {

    public static IngresoResponse from(Ingreso ingreso, List<LoteResponse> items) {
        return new IngresoResponse(
                ingreso.getId(),
                ingreso.getFecha(),
                ingreso.getDocumento(),
                ingreso.getProveedor() == null ? null : ingreso.getProveedor().getRazonSocial(),
                ingreso.getObservaciones(),
                ingreso.getTotal(),
                ingreso.getUsuario().getNombreCompleto(),
                items);
    }
}
