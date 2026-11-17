package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.UsuarioAdminResponse;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.UsuarioAdminService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioAdminServiceImpl implements UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final SesionUsuarioRepository sesionRepository;

    @Override
    public List<UsuarioAdminResponse> listar(RolNombre rol, String busqueda) {
        String termino = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        Map<Long, LocalDateTime> accesos = ultimosAccesos();
        return usuarioRepository.findAllConRoles().stream()
                .filter(usuario -> rol == null || usuario.getRoles().stream().anyMatch(r -> r.getNombre() == rol))
                .filter(usuario -> termino.isEmpty() || coincide(usuario, termino))
                .map(usuario -> UsuarioAdminResponse.from(usuario, accesos.get(usuario.getId())))
                .toList();
    }

    @Override
    @Transactional
    public UsuarioAdminResponse cambiarEstado(Long usuarioId, EstadoUsuario estado, UsuarioPrincipal actor) {
        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));
        if (usuario.getId().equals(actor.getId()) && estado == EstadoUsuario.INACTIVO) {
            throw new BusinessLogicException("Un administrador no puede desactivar su propia cuenta");
        }
        usuario.setEstado(estado);
        usuarioRepository.saveAndFlush(usuario);
        if (estado == EstadoUsuario.INACTIVO) {
            sesionRepository.revocarTodasDelUsuario(usuarioId);
        }
        log.info("Usuario {} cambió a {} por {}", usuario.getEmail(), estado, actor.getEmail());
        return UsuarioAdminResponse.from(usuario, ultimosAccesos().get(usuarioId));
    }

    private Map<Long, LocalDateTime> ultimosAccesos() {
        Map<Long, LocalDateTime> accesos = new HashMap<>();
        for (Object[] fila : sesionRepository.findUltimoAccesoPorUsuario()) {
            accesos.put((Long) fila[0], (LocalDateTime) fila[1]);
        }
        return accesos;
    }

    private static boolean coincide(Usuario usuario, String termino) {
        return usuario.getNombre().toLowerCase(Locale.ROOT).contains(termino)
                || usuario.getApellido().toLowerCase(Locale.ROOT).contains(termino)
                || usuario.getEmail().toLowerCase(Locale.ROOT).contains(termino);
    }
}
