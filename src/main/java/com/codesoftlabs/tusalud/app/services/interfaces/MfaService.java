package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.MfaCodigosRecuperacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaConfiguracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaEstadoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;

public interface MfaService {

    MfaEstadoResponse estado(UsuarioPrincipal actor);

    MfaConfiguracionResponse configurar(UsuarioPrincipal actor);

    MfaCodigosRecuperacionResponse activar(UsuarioPrincipal actor, String codigo);

    void desactivar(UsuarioPrincipal actor, String password, String codigo);

    MfaCodigosRecuperacionResponse regenerarCodigos(UsuarioPrincipal actor, String codigo);

    boolean estaActivo(Long usuarioId);

    void validarSegundoPaso(Long usuarioId, String codigo);
}
