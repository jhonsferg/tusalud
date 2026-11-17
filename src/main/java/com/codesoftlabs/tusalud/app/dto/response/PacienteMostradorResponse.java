package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Paciente;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Paciente encontrado en el mostrador")
public record PacienteMostradorResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID pacienteId,

        @Schema(example = "Juan Pérez") String nombreCompleto,
        @Schema(example = "12345678") String dni,
        @Schema(example = "987654321") String telefono,
        @Schema(example = "juan.perez@correo.com") String email,
        @Schema(type = "string", example = "1990-05-15") LocalDate fechaNacimiento,
        @Schema(example = "HC-000001") String historialClinicoNumero) {

    public static PacienteMostradorResponse minimo(
            UUID pacienteId, String nombreCompleto, String dni, String telefono) {
        return new PacienteMostradorResponse(pacienteId, nombreCompleto, dni, telefono, null, null, null);
    }

    public PacienteMostradorResponse sinDatosPersonales() {
        return minimo(pacienteId, nombreCompleto, dni, telefono);
    }

    public static PacienteMostradorResponse from(Paciente paciente) {
        var usuario = paciente.getUsuario();
        return new PacienteMostradorResponse(
                paciente.getId(),
                usuario.getNombreCompleto(),
                paciente.getDni(),
                usuario.getTelefono(),
                usuario.getEmail(),
                paciente.getFechaNacimiento(),
                paciente.getHistorialClinicoNumero());
    }
}
