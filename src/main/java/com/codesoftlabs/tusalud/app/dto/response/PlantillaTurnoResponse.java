package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.PlantillaTurno;
import java.time.LocalTime;
import java.util.UUID;

public record PlantillaTurnoResponse(
        UUID id, String nombre, LocalTime horaInicio, LocalTime horaFin, double horas, boolean cruzaMedianoche) {

    public static PlantillaTurnoResponse from(PlantillaTurno p) {
        return new PlantillaTurnoResponse(
                p.getId(),
                p.getNombre(),
                p.getHoraInicio(),
                p.getHoraFin(),
                p.duracion().toMinutes() / 60.0,
                p.cruzaMedianoche());
    }
}
