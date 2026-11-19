package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoTeleconsulta;
import java.time.LocalDateTime;
import java.util.UUID;

public record TeleconsultaResponse(
        UUID id,
        UUID citaId,
        LocalDateTime fechaHora,
        int duracionMinutos,
        EstadoTeleconsulta estado,
        String medicoNombre,
        String especialidad,
        String pacienteNombre,
        String motivo,
        String rol,
        LocalDateTime ventanaDesde,
        LocalDateTime ventanaHasta,
        boolean puedeIngresar,
        boolean salaAbierta,
        boolean consentimientoPendiente,
        Integer duracionSegundos) {}
