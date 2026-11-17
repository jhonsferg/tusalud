package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.ProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.response.ProveedorResponse;
import com.codesoftlabs.tusalud.app.entities.Proveedor;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ProveedorRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.ProveedorService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Override
    public List<ProveedorResponse> listar(String q, Boolean activo) {
        String termino = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return proveedorRepository.buscar(termino, activo).stream()
                .map(ProveedorResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        String ruc = request.ruc().trim();
        if (proveedorRepository.existsByRuc(ruc)) {
            throw new ConflictException("Ya existe un proveedor con el RUC " + ruc);
        }
        Proveedor proveedor = Proveedor.builder().ruc(ruc).build();
        aplicar(proveedor, request);
        return ProveedorResponse.from(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = proveedorRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", id));
        String ruc = request.ruc().trim();
        if (proveedorRepository.existsByRucAndIdNot(ruc, id)) {
            throw new ConflictException("Ya existe un proveedor con el RUC " + ruc);
        }
        proveedor.setRuc(ruc);
        aplicar(proveedor, request);
        return ProveedorResponse.from(proveedor);
    }

    private void aplicar(Proveedor proveedor, ProveedorRequest request) {
        proveedor.setRazonSocial(request.razonSocial().trim());
        proveedor.setContacto(limpiar(request.contacto()));
        proveedor.setTelefono(limpiar(request.telefono()));
        proveedor.setEmail(limpiar(request.email()));
        proveedor.setDireccion(limpiar(request.direccion()));
        proveedor.setActivo(request.activo() == null || request.activo());
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
