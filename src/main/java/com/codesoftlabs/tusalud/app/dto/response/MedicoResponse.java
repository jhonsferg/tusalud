package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Médico con sus especialidades")
public record MedicoResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID usuarioId,

        @Schema(example = "Luis") String nombre,

        @Schema(example = "García") String apellido,

        @Schema(example = "Luis García") String nombreCompleto,

        @Schema(example = "dr.garcia@tusalud.com") String email,

        @Schema(example = "912345678") String telefono,

        @Schema(example = "CMP-12345") String colegiatura,

        @Schema(example = "Consultorio 101") String consultorio,

        @Schema(description = "Especialidades que atiende, con el precio de su consulta")
        List<EspecialidadResponse> especialidades,

        @Schema(example = "ACTIVO") EstadoUsuario estado) {

    public MedicoResponse paraActor(java.util.UUID usuarioActorId, boolean administrador) {
        if (administrador || usuarioId.equals(usuarioActorId)) {
            return this;
        }
        return new MedicoResponse(
                id,
                usuarioId,
                nombre,
                apellido,
                nombreCompleto,
                null,
                null,
                colegiatura,
                consultorio,
                especialidades,
                estado);
    }

    public static MedicoResponse from(Medico medico) {
        Usuario usuario = medico.getUsuario();
        return new MedicoResponse(
                medico.getId(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getTelefono(),
                medico.getColegiatura(),
                medico.getConsultorio(),
                medico.getEspecialidades().stream()
                        .map(EspecialidadResponse::from)
                        .toList(),
                usuario.getEstado());
    }
}
