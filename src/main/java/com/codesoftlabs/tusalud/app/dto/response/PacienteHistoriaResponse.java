package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Paciente;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.Period;

@Schema(description = "Datos del paciente dentro de la historia clínica")
public record PacienteHistoriaResponse(
        @Schema(example = "1") Long pacienteId,
        @Schema(example = "Juan Pérez") String nombreCompleto,
        @Schema(example = "12345678") String dni,
        @Schema(example = "HC-000001") String historialClinicoNumero,
        @Schema(type = "string", example = "1990-05-15") LocalDate fechaNacimiento,
        @Schema(example = "36") Integer edad,
        @Schema(example = "O+") String grupoSanguineo,
        @Schema(example = "Penicilina") String alergias,
        @Schema(example = "Hipertensión familiar") String antecedentes) {

    public static PacienteHistoriaResponse from(Paciente paciente) {
        LocalDate nacimiento = paciente.getFechaNacimiento();
        return new PacienteHistoriaResponse(
                paciente.getId(),
                paciente.getUsuario().getNombreCompleto(),
                paciente.getDni(),
                paciente.getHistorialClinicoNumero(),
                nacimiento,
                nacimiento == null
                        ? null
                        : Period.between(nacimiento, LocalDate.now()).getYears(),
                paciente.getGrupoSanguineo(),
                paciente.getAlergias(),
                paciente.getAntecedentes());
    }
}
