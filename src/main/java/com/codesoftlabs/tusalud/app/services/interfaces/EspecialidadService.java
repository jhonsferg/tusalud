package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.EspecialidadRequest;
import com.codesoftlabs.tusalud.app.dto.response.EspecialidadResponse;
import java.util.List;
import java.util.UUID;

public interface EspecialidadService {

    List<EspecialidadResponse> listar();

    EspecialidadResponse obtener(UUID id);

    EspecialidadResponse crear(EspecialidadRequest request);

    EspecialidadResponse actualizar(UUID id, EspecialidadRequest request);

    void eliminar(UUID id);
}
