package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Proveedor;
import java.util.UUID;

public record ProveedorResponse(
        UUID id,
        String ruc,
        String razonSocial,
        String contacto,
        String telefono,
        String email,
        String direccion,
        boolean activo,
        int condicionPagoDias,
        Integer plazoEntregaDias,
        String banco,
        String cuentaBancaria,
        String notas) {

    public static ProveedorResponse from(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getRuc(),
                proveedor.getRazonSocial(),
                proveedor.getContacto(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getDireccion(),
                proveedor.isActivo(),
                proveedor.getCondicionPagoDias(),
                proveedor.getPlazoEntregaDias(),
                proveedor.getBanco(),
                proveedor.getCuentaBancaria(),
                proveedor.getNotas());
    }
}
