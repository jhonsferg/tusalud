package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResumenResponse;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface NotificacionService {

    void crear(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje, String enlace);

    List<NotificacionResponse> listar(UsuarioPrincipal actor, boolean soloNoLeidas, int limite);

    NotificacionResumenResponse resumen(UsuarioPrincipal actor);

    NotificacionResponse marcarLeida(UsuarioPrincipal actor, UUID id);

    void marcarTodasLeidas(UsuarioPrincipal actor);

    void eliminar(UsuarioPrincipal actor, UUID id);
}
