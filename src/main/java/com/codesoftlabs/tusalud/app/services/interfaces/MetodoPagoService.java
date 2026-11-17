package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.MetodoPagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.MetodoPagoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;

public interface MetodoPagoService {

    List<MetodoPagoResponse> listar(UsuarioPrincipal actor);

    MetodoPagoResponse crear(UsuarioPrincipal actor, MetodoPagoRequest request);

    MetodoPagoResponse marcarPredeterminado(UsuarioPrincipal actor, Long id);

    void eliminar(UsuarioPrincipal actor, Long id);
}
