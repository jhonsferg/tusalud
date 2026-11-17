package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.MetodoPagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.MetodoPagoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface MetodoPagoService {

    List<MetodoPagoResponse> listar(UsuarioPrincipal actor);

    MetodoPagoResponse crear(UsuarioPrincipal actor, MetodoPagoRequest request);

    MetodoPagoResponse marcarPredeterminado(UsuarioPrincipal actor, UUID id);

    void eliminar(UsuarioPrincipal actor, UUID id);
}
