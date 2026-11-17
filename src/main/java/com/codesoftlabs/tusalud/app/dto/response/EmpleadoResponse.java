package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.entities.enums.TipoContrato;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ficha laboral; el sueldo y el contacto de emergencia solo llegan a quien tiene permiso")
public record EmpleadoResponse(
        UUID id,
        UUID usuarioId,
        String nombreCompleto,
        String email,
        List<String> roles,
        String cargo,
        AreaLaboral area,
        TipoContrato tipoContrato,
        LocalDate fechaIngreso,
        LocalDate fechaFinContrato,
        EstadoEmpleado estado,
        @Schema(description = "Solo para RRHH y GERENCIA") BigDecimal sueldo,
        @Schema(description = "No llega al administrador") String contactoEmergenciaNombre,
        @Schema(description = "No llega al administrador") String contactoEmergenciaTelefono,
        int diasVacacionesAnuales) {

    public static EmpleadoResponse from(Empleado e, List<String> roles, boolean verSueldo, boolean verContacto) {
        return new EmpleadoResponse(
                e.getId(),
                e.getUsuario().getId(),
                e.getUsuario().getNombreCompleto(),
                e.getUsuario().getEmail(),
                roles,
                e.getCargo(),
                e.getArea(),
                e.getTipoContrato(),
                e.getFechaIngreso(),
                e.getFechaFinContrato(),
                e.getEstado(),
                verSueldo ? e.getSueldo() : null,
                verContacto ? e.getContactoEmergenciaNombre() : null,
                verContacto ? e.getContactoEmergenciaTelefono() : null,
                e.getDiasVacacionesAnuales());
    }
}
