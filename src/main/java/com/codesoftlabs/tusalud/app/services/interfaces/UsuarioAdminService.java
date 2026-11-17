package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.PersonalCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.RolResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsuarioAdminResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface UsuarioAdminService {

    List<UsuarioAdminResponse> listar(RolNombre rol, String busqueda);

    List<RolResponse> listarRoles();

    UsuarioAdminResponse asignarRoles(UUID usuarioId, List<RolNombre> roles, UsuarioPrincipal actor);

    UsuarioAdminResponse crearPersonal(PersonalCreateRequest request, UsuarioPrincipal actor);

    UsuarioAdminResponse cambiarEstado(UUID usuarioId, EstadoUsuario estado, UsuarioPrincipal actor);
}
