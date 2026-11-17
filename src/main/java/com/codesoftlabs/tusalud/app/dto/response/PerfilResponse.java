package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Datos personales del usuario autenticado y, según su rol, los de paciente o médico")
public record PerfilResponse(
        @Schema(example = "4") Long usuarioId,
        @Schema(example = "Juan") String nombre,
        @Schema(example = "Pérez") String apellido,
        @Schema(example = "Juan Pérez") String nombreCompleto,
        @Schema(example = "juan.perez@correo.com") String email,
        @Schema(example = "987654321") String telefono,
        @Schema(example = "[\"ROLE_PACIENTE\"]") List<String> roles,

        @Schema(type = "string", example = "2026-10-01T10:15:30")
        LocalDateTime createdAt,

        @Schema(example = "12345678") String dni,
        @Schema(type = "string", example = "1992-04-15") LocalDate fechaNacimiento,
        @Schema(example = "HC-000001") String historialClinicoNumero,
        @Schema(example = "1") Long medicoId,
        @Schema(example = "CMP-12345") String colegiatura,
        @Schema(example = "Consultorio 101") String consultorio,
        @Schema(example = "Cardiología") String especialidad,
        @Schema(example = "150.00") BigDecimal precioConsulta) {

    public static PerfilResponse from(Usuario usuario, List<String> roles, Paciente paciente, Medico medico) {
        return new PerfilResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getTelefono(),
                roles,
                usuario.getCreatedAt(),
                paciente == null ? null : paciente.getDni(),
                paciente == null ? null : paciente.getFechaNacimiento(),
                paciente == null ? null : paciente.getHistorialClinicoNumero(),
                medico == null ? null : medico.getId(),
                medico == null ? null : medico.getColegiatura(),
                medico == null ? null : medico.getConsultorio(),
                medico == null ? null : medico.getEspecialidad().getNombre(),
                medico == null ? null : medico.getPrecioConsulta());
    }
}
