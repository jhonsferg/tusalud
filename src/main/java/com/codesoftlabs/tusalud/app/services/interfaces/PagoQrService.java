package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.PagoQrRequest;
import com.codesoftlabs.tusalud.app.dto.response.PagoQrResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;

public interface PagoQrService {

    PagoQrResponse crear(PagoQrRequest request, UsuarioPrincipal actor);

    PagoQrResponse obtener(String codigo, UsuarioPrincipal actor);

    PagoQrResponse simularConfirmacion(String codigo, UsuarioPrincipal actor);

    PagoQrResponse cancelar(String codigo, UsuarioPrincipal actor);
}
