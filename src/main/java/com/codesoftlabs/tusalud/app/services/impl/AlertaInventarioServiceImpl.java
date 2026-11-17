package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.AlertasInventarioResponse;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.AlertaInventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaInventarioServiceImpl implements AlertaInventarioService {

    private static final String ENLACE = "/almacen/alertas";

    private final AlmacenService almacenService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    @Scheduled(cron = "${app.almacen.cron-alertas:0 0 7 * * *}")
    @Transactional
    public void ejecutarProgramado() {
        int enviadas = notificarAlertas(LocalDateTime.now());
        log.debug("Alertas de inventario enviadas: {}", enviadas);
    }

    @Override
    @Transactional
    public int notificarAlertas(LocalDateTime ahora) {
        AlertasInventarioResponse alertas = almacenService.alertas();
        boolean hayBajoStock = !alertas.bajoStock().isEmpty();
        boolean hayPorVencer =
                !alertas.porVencer().isEmpty() || !alertas.vencidos().isEmpty();
        if (!hayBajoStock && !hayPorVencer) {
            return 0;
        }
        LocalDateTime inicioDia = ahora.toLocalDate().atStartOfDay();
        List<Usuario> destinatarios = usuarioRepository.findActivosPorRol(RolNombre.ROLE_ALMACEN);
        int enviadas = 0;
        for (Usuario usuario : destinatarios) {
            if (hayBajoStock && !yaNotificado(usuario, TipoNotificacion.STOCK_BAJO, inicioDia)) {
                notificacionService.crear(
                        usuario,
                        TipoNotificacion.STOCK_BAJO,
                        "Productos con stock bajo",
                        alertas.bajoStock().size() + " productos están en o por debajo de su stock mínimo.",
                        ENLACE);
                enviadas++;
            }
            if (hayPorVencer && !yaNotificado(usuario, TipoNotificacion.PRODUCTO_POR_VENCER, inicioDia)) {
                notificacionService.crear(
                        usuario,
                        TipoNotificacion.PRODUCTO_POR_VENCER,
                        "Lotes por vencer o vencidos",
                        alertas.porVencer().size() + " lotes vencen pronto y "
                                + alertas.vencidos().size() + " ya vencieron.",
                        ENLACE);
                enviadas++;
            }
        }
        return enviadas;
    }

    private boolean yaNotificado(Usuario usuario, TipoNotificacion tipo, LocalDateTime desde) {
        return notificacionRepository.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                usuario.getId(), tipo, desde);
    }
}
