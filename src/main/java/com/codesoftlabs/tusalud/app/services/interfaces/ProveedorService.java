package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.ProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.response.ProveedorResponse;
import java.util.List;

public interface ProveedorService {

    List<ProveedorResponse> listar(String q, Boolean activo);

    ProveedorResponse crear(ProveedorRequest request);

    ProveedorResponse actualizar(Long id, ProveedorRequest request);
}
