package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResumenResponse;
import com.codesoftlabs.tusalud.app.entities.Notificacion;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.events.NotificacionCreadaEvent;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void crear(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje, String enlace) {
        Notificacion guardada = notificacionRepository.saveAndFlush(Notificacion.builder()
                .usuario(usuario)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .enlace(enlace)
                .build());
        eventPublisher.publishEvent(new NotificacionCreadaEvent(usuario.getId(), NotificacionResponse.from(guardada)));
    }

    @Override
    public List<NotificacionResponse> listar(UsuarioPrincipal actor, boolean soloNoLeidas, int limite) {
        PageRequest pagina = PageRequest.of(0, limite);
        List<Notificacion> notificaciones = soloNoLeidas
                ? notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreatedAtDescIdDesc(actor.getId(), pagina)
                : notificacionRepository.findByUsuarioIdOrderByCreatedAtDescIdDesc(actor.getId(), pagina);
        return notificaciones.stream().map(NotificacionResponse::from).toList();
    }

    @Override
    public NotificacionResumenResponse resumen(UsuarioPrincipal actor) {
        return new NotificacionResumenResponse(notificacionRepository.countByUsuarioIdAndLeidaFalse(actor.getId()));
    }

    @Override
    @Transactional
    public NotificacionResponse marcarLeida(UsuarioPrincipal actor, Long id) {
        Notificacion notificacion = buscarDelUsuario(actor, id);
        notificacion.setLeida(true);
        return NotificacionResponse.from(notificacionRepository.saveAndFlush(notificacion));
    }

    @Override
    @Transactional
    public void marcarTodasLeidas(UsuarioPrincipal actor) {
        notificacionRepository.marcarTodasComoLeidas(actor.getId());
    }

    @Override
    @Transactional
    public void eliminar(UsuarioPrincipal actor, Long id) {
        notificacionRepository.delete(buscarDelUsuario(actor, id));
    }

    private Notificacion buscarDelUsuario(UsuarioPrincipal actor, Long id) {
        return notificacionRepository
                .findByIdAndUsuarioId(id, actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", "id", id));
    }
}
