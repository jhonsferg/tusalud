package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.InformeImagen;
import java.time.LocalDateTime;
import java.util.UUID;

public record InformeImagenResponse(
        UUID id,
        int version,
        String hallazgos,
        String conclusion,
        Boolean hallazgoCritico,
        String firmaNombre,
        String firmaColegiatura,
        String motivoCorreccion,
        LocalDateTime fecha) {

    public static InformeImagenResponse completo(InformeImagen informe) {
        return new InformeImagenResponse(
                informe.getId(),
                informe.getVersion(),
                informe.getHallazgos(),
                informe.getConclusion(),
                informe.isHallazgoCritico(),
                informe.getFirmaNombre(),
                informe.getFirmaColegiatura(),
                informe.getMotivoCorreccion(),
                informe.getCreatedAt());
    }

    public static InformeImagenResponse paraPaciente(InformeImagen informe) {
        return new InformeImagenResponse(
                informe.getId(),
                informe.getVersion(),
                informe.getHallazgos(),
                informe.getConclusion(),
                null,
                informe.getFirmaNombre(),
                informe.getFirmaColegiatura(),
                null,
                informe.getCreatedAt());
    }
}
