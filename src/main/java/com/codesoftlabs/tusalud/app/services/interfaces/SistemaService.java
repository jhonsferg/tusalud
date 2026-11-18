package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.EstadoSistemaResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsoAlmacenamientoResponse;

public interface SistemaService {

    EstadoSistemaResponse estado();

    UsoAlmacenamientoResponse usoAlmacenamiento();
}
