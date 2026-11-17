package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.RolResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsuarioAdminResponse;
import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.UsuarioAdminService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final RolRepository rolRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final NotificacionService notificacionService;

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
    public List<RolResponse> listarRoles() {
        return Arrays.stream(RolNombre.values())
                .map(rol -> new RolResponse(rol.name(), descripcion(rol), usuarioRepository.contarPorRol(rol)))
                .toList();
    }

    @Override
    @Transactional
    public UsuarioAdminResponse asignarRoles(Long usuarioId, List<RolNombre> roles, UsuarioPrincipal actor) {
        Usuario usuario = usuarioRepository
                .findByIdConRoles(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));
        Set<RolNombre> nuevos = EnumSet.copyOf(roles);
        Set<RolNombre> actuales =
                usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());

        if (actuales.contains(RolNombre.ROLE_ADMIN) && !nuevos.contains(RolNombre.ROLE_ADMIN)) {
            if (usuario.getId().equals(actor.getId())) {
                throw new BusinessLogicException(
                        "Un administrador no puede quitarse a sí mismo el rol de administrador");
            }
            if (usuarioRepository.contarPorRol(RolNombre.ROLE_ADMIN) <= 1) {
                throw new BusinessLogicException("El sistema debe conservar al menos un administrador");
            }
        }
        if (nuevos.contains(RolNombre.ROLE_MEDICO)
                && medicoRepository.findByUsuarioEmail(usuario.getEmail()).isEmpty()) {
            throw new BusinessLogicException(
                    "El usuario no tiene un perfil de médico. Regístrelo desde el módulo de médicos");
        }
        if (nuevos.contains(RolNombre.ROLE_PACIENTE)
                && pacienteRepository.findByUsuarioEmail(usuario.getEmail()).isEmpty()) {
            throw new BusinessLogicException("El usuario no tiene un perfil de paciente");
        }

        usuario.getRoles().clear();
        for (RolNombre nombre : nuevos) {
            usuario.getRoles()
                    .add(rolRepository
                            .findByNombre(nombre)
                            .orElseGet(() -> rolRepository.save(
                                    Rol.builder().nombre(nombre).build())));
        }
        usuarioRepository.saveAndFlush(usuario);
        sesionRepository.revocarTodasDelUsuario(usuarioId);
        notificacionService.crear(
                usuario,
                TipoNotificacion.ROLES_ACTUALIZADOS,
                "Tus roles fueron actualizados",
                "Un administrador modificó tus permisos. Vuelve a iniciar sesión para aplicarlos.",
                "/perfil");
        log.info("Roles de {} actualizados a {} por {}", usuario.getEmail(), nuevos, actor.getEmail());
        return UsuarioAdminResponse.from(usuario, ultimosAccesos().get(usuarioId));
    }

    private static String descripcion(RolNombre rol) {
        return switch (rol) {
            case ROLE_ADMIN -> "Gestiona usuarios, roles, especialidades, precios, cupones y reportes";
            case ROLE_MEDICO -> "Atiende pacientes, registra atenciones y comparte historias clínicas";
            case ROLE_PACIENTE -> "Reserva citas, paga consultas y consulta su historia clínica";
        };
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
