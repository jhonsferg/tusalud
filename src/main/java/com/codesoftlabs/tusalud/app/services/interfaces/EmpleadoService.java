package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.EmpleadoRequest;
import com.codesoftlabs.tusalud.app.dto.response.EmpleadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RrhhResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsuarioDisponibleResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface EmpleadoService {

    List<EmpleadoResponse> listar(String q, AreaLaboral area, EstadoEmpleado estado, UsuarioPrincipal actor);

    EmpleadoResponse obtener(UUID id, UsuarioPrincipal actor);

    EmpleadoResponse crear(EmpleadoRequest request, UsuarioPrincipal actor);

    EmpleadoResponse actualizar(UUID id, EmpleadoRequest request, UsuarioPrincipal actor);

    EmpleadoResponse miFicha(UsuarioPrincipal actor);

    List<UsuarioDisponibleResponse> usuariosDisponibles();

    RrhhResumenResponse resumen(UsuarioPrincipal actor);
}
