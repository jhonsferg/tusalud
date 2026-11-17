package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.ProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.response.ProveedorResponse;
import java.util.List;
import java.util.UUID;

public interface ProveedorService {

    List<ProveedorResponse> listar(String q, Boolean activo);

    ProveedorResponse crear(ProveedorRequest request);

    ProveedorResponse actualizar(UUID id, ProveedorRequest request);
}
