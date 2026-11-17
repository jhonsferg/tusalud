package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Proveedor;

public record ProveedorResponse(
        Long id,
        String ruc,
        String razonSocial,
        String contacto,
        String telefono,
        String email,
        String direccion,
        boolean activo) {

    public static ProveedorResponse from(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getRuc(),
                proveedor.getRazonSocial(),
                proveedor.getContacto(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getDireccion(),
                proveedor.isActivo());
    }
}
