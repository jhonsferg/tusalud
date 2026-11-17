package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.TurnoPersonal;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TurnoPersonalResponse(
        UUID id,
        UUID empleadoId,
        String empleadoNombre,
        AreaLaboral area,
        UUID plantillaId,
        String plantillaNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        double horas,
        String notas) {

    public static TurnoPersonalResponse from(TurnoPersonal t) {
        return new TurnoPersonalResponse(
                t.getId(),
                t.getEmpleado().getId(),
                t.getEmpleado().getUsuario().getNombreCompleto(),
                t.getEmpleado().getArea(),
                t.getPlantilla().getId(),
                t.getPlantilla().getNombre(),
                t.getFecha(),
                t.getPlantilla().getHoraInicio(),
                t.getPlantilla().getHoraFin(),
                t.getPlantilla().duracion().toMinutes() / 60.0,
                t.getNotas());
    }
}
