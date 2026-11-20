package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanillaAvisos {

    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public void avisarRol(
            RolNombre rol, UUID excluir, TipoNotificacion tipo, String titulo, String mensaje, String enlace) {
        for (Usuario destinatario : usuarioRepository.findActivosPorRol(rol)) {
            if (excluir != null && excluir.equals(destinatario.getId())) {
                continue;
            }
            notificacionService.crear(destinatario, tipo, titulo, mensaje, enlace);
        }
    }

    public void avisarUsuario(
            Usuario destinatario, TipoNotificacion tipo, String titulo, String mensaje, String enlace) {
        notificacionService.crear(destinatario, tipo, titulo, mensaje, enlace);
    }
}
