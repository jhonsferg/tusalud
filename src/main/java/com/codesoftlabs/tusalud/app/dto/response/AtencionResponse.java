package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Atencion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Atención médica registrada para una cita")
public record AtencionResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID citaId,

        @Schema(type = "string", example = "2026-10-19T09:30:00")
        LocalDateTime fecha,

        @Schema(example = "Luis García") String medicoNombre,
        @Schema(example = "Cardiología") String especialidad,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID pacienteId,

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
        List<RecetaItemResponse> receta,
        List<DiagnosticoResponse> diagnosticos,

        @Schema(description = "Avisos que no bloquean el registro, por ejemplo restricciones de sexo o edad del código")
        List<String> advertencias) {

    public AtencionResponse conAdvertencias(List<String> avisos) {
        return new AtencionResponse(
                id,
                citaId,
                fecha,
                medicoNombre,
                especialidad,
                pacienteId,
                pacienteNombre,
                diagnostico,
                tratamiento,
                indicaciones,
                observaciones,
                pesoKg,
                tallaCm,
                presionArterial,
                temperaturaC,
                frecuenciaCardiaca,
                receta,
                diagnosticos,
                avisos);
    }

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
                atencion.getReceta().stream().map(RecetaItemResponse::from).toList(),
                atencion.getDiagnosticos().stream()
                        .map(DiagnosticoResponse::from)
                        .toList(),
                List.of());
    }
}
