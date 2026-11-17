package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.UUID;

public interface AuditoriaRrhhService {

    String EMPLEADO_CREADO = "EMPLEADO_CREADO";
    String EMPLEADO_ACTUALIZADO = "EMPLEADO_ACTUALIZADO";
    String TURNO_ASIGNADO = "TURNO_ASIGNADO";
    String TURNO_ELIMINADO = "TURNO_ELIMINADO";
    String AUSENCIA_APROBADA = "AUSENCIA_APROBADA";
    String AUSENCIA_RECHAZADA = "AUSENCIA_RECHAZADA";
    String AUSENCIA_CANCELADA = "AUSENCIA_CANCELADA";

    void registrar(UsuarioPrincipal actor, UUID empleadoId, String accion, String detalle);
}
