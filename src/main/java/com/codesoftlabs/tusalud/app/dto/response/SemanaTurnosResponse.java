package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SemanaTurnosResponse(LocalDate desde, LocalDate hasta, AreaLaboral area, List<Fila> filas) {

    public record Fila(
            UUID empleadoId,
            String nombreCompleto,
            String cargo,
            AreaLaboral area,
            double horasSemana,
            List<Dia> dias) {}

    public record Dia(LocalDate fecha, List<TurnoPersonalResponse> turnos, TipoAusencia ausencia) {}
}
