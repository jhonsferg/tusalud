package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CobroRequest(
        @NotNull(message = "El origen del cobro es obligatorio")
        OrigenComprobante origen,

        UUID referenciaId,
        UUID pacienteId,

        @NotNull(message = "El tipo de comprobante es obligatorio")
        TipoComprobante tipoComprobante,

        @NotNull(message = "El tipo de documento del cliente es obligatorio")
        TipoDocumentoCliente clienteTipoDoc,

        @Size(max = 11, message = "El documento del cliente no puede superar 11 caracteres")
        String clienteDoc,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 150, message = "El nombre del cliente no puede superar 150 caracteres")
        String clienteNombre,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPagoCaja metodoPago,

        @Valid List<CobroItemRequest> items) {}
