package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Comprobante;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ComprobanteResponse(
        UUID id,
        TipoComprobante tipo,
        String serie,
        Integer numero,
        String codigo,
        LocalDateTime fecha,
        TipoDocumentoCliente clienteTipoDoc,
        String clienteDoc,
        String clienteNombre,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        MetodoPagoCaja metodoPago,
        EstadoComprobante estado,
        String motivoAnulacion,
        OrigenComprobante origen,
        UUID referenciaId,
        UUID pacienteId,
        UUID cajaId,
        String cajeroNombre,
        List<ComprobanteItemResponse> items,
        BigDecimal montoCubierto,
        String aseguradoraNombre) {

    public static ComprobanteResponse from(Comprobante c) {
        return new ComprobanteResponse(
                c.getId(),
                c.getTipo(),
                c.getSerie(),
                c.getNumero(),
                c.getCodigo(),
                c.getFecha(),
                c.getClienteTipoDoc(),
                c.getClienteDoc(),
                c.getClienteNombre(),
                c.getSubtotal(),
                c.getIgv(),
                c.getTotal(),
                c.getMetodoPago(),
                c.getEstado(),
                c.getMotivoAnulacion(),
                c.getOrigen(),
                c.getReferenciaId(),
                c.getPaciente() == null ? null : c.getPaciente().getId(),
                c.getCaja().getId(),
                c.getCaja().getCajero().getNombreCompleto(),
                c.getItems().stream().map(ComprobanteItemResponse::from).toList(),
                c.getMontoCubierto(),
                c.getAseguradoraNombre());
    }
}
