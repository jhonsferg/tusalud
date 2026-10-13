package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Médico con su especialidad")
public record MedicoResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "2") Long usuarioId,

        @Schema(example = "Luis") String nombre,

        @Schema(example = "García") String apellido,

        @Schema(example = "Luis García") String nombreCompleto,

        @Schema(example = "dr.garcia@tusalud.com") String email,

        @Schema(example = "912345678") String telefono,

        @Schema(example = "CMP-12345") String colegiatura,

        @Schema(example = "Consultorio 101") String consultorio,

        @Schema(example = "150.00") BigDecimal precioConsulta,

        @Schema(example = "1") Long especialidadId,

        @Schema(example = "Cardiología") String especialidadNombre,

        @Schema(example = "ACTIVO") EstadoUsuario estado) {

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
                medico.getPrecioConsulta(),
                medico.getEspecialidad().getId(),
                medico.getEspecialidad().getNombre(),
                usuario.getEstado());
    }
}
