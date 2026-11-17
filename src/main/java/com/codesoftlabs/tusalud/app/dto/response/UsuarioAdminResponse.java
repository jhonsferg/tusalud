package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Cuenta de usuario vista por un administrador")
public record UsuarioAdminResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "Juan Pérez") String nombreCompleto,
        @Schema(example = "juan.perez@correo.com") String email,
        @Schema(example = "987654321") String telefono,
        @Schema(example = "[\"ROLE_PACIENTE\"]") List<String> roles,
        @Schema(example = "ACTIVO") EstadoUsuario estado,

        @Schema(type = "string", example = "2026-10-01T10:15:30")
        LocalDateTime createdAt,

        @Schema(type = "string", example = "2026-10-19T09:00:00")
        LocalDateTime ultimoAcceso) {

    public static UsuarioAdminResponse from(Usuario usuario, LocalDateTime ultimoAcceso) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRoles().stream()
                        .map(Rol::getNombre)
                        .map(Enum::name)
                        .sorted()
                        .toList(),
                usuario.getEstado(),
                usuario.getCreatedAt(),
                ultimoAcceso);
    }
}
