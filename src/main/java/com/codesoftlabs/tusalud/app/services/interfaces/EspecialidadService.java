package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.EspecialidadRequest;
import com.codesoftlabs.tusalud.app.dto.response.EspecialidadResponse;
import java.util.List;

public interface EspecialidadService {

    List<EspecialidadResponse> listar();

    EspecialidadResponse obtener(Long id);

    EspecialidadResponse crear(EspecialidadRequest request);

    EspecialidadResponse actualizar(Long id, EspecialidadRequest request);

    void eliminar(Long id);
}
