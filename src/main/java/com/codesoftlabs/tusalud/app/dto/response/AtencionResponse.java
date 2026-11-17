package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Atencion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Atención médica registrada para una cita")
public record AtencionResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "10") Long citaId,

        @Schema(type = "string", example = "2026-10-19T09:30:00")
        LocalDateTime fecha,

        @Schema(example = "Luis García") String medicoNombre,
        @Schema(example = "Cardiología") String especialidad,
        @Schema(example = "1") Long pacienteId,
        @Schema(example = "Juan Pérez") String pacienteNombre,
        @Schema(example = "Hipertensión arterial leve") String diagnostico,
        @Schema(example = "Control con dieta baja en sal") String tratamiento,
        @Schema(example = "Reposo relativo") String indicaciones,
        @Schema(example = "Paciente refiere mejoría") String observaciones,
        @Schema(example = "72.5") BigDecimal pesoKg,
        @Schema(example = "170.0") BigDecimal tallaCm,
        @Schema(example = "120/80") String presionArterial,
        @Schema(example = "36.5") BigDecimal temperaturaC,
        @Schema(example = "72") Integer frecuenciaCardiaca,
        List<RecetaItemResponse> receta) {

    public static AtencionResponse from(Atencion atencion) {
        return new AtencionResponse(
                atencion.getId(),
                atencion.getCita().getId(),
                atencion.getFecha(),
                atencion.getMedico().getUsuario().getNombreCompleto(),
                atencion.getCita().getEspecialidad().getNombre(),
                atencion.getPaciente().getId(),
                atencion.getPaciente().getUsuario().getNombreCompleto(),
                atencion.getDiagnostico(),
                atencion.getTratamiento(),
                atencion.getIndicaciones(),
                atencion.getObservaciones(),
                atencion.getPesoKg(),
                atencion.getTallaCm(),
                atencion.getPresionArterial(),
                atencion.getTemperaturaC(),
                atencion.getFrecuenciaCardiaca(),
                atencion.getReceta().stream().map(RecetaItemResponse::from).toList());
    }
}
