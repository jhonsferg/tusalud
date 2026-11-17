package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.SesionResponse;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface SesionService {

    UUID iniciarSesion(Usuario usuario, ContextoConexion contexto);

    boolean sesionActiva(UUID tokenId);

    List<SesionResponse> listarActivas(UsuarioPrincipal actor, UUID tokenActual);

    void revocar(UsuarioPrincipal actor, UUID sesionId);

    void revocarTodasExceptoActual(UsuarioPrincipal actor, UUID tokenActual);

    void cerrarSesion(UUID tokenId);

    void revocarTodasDelUsuario(UUID usuarioId);
}
