package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Médico con quien se comparte un paciente")
public record PacienteCompartidoResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID medicoId,

        @Schema(example = "Ricardo Salazar") String medicoNombre,

        @Schema(example = "[\"Medicina General\"]") List<String> especialidades,

        @Schema(example = "Requiere valoración complementaria")
        String nota,

        @Schema(type = "string", example = "2026-10-19T09:00:00")
        LocalDateTime fecha) {

    public static PacienteCompartidoResponse from(PacienteCompartido compartido) {
        Medico destino = compartido.getMedicoDestino();
        return new PacienteCompartidoResponse(
                destino.getId(),
                destino.getUsuario().getNombreCompleto(),
                destino.getEspecialidades().stream()
                        .map(Especialidad::getNombre)
                        .toList(),
                compartido.getNota(),
                compartido.getCreatedAt());
    }
}
