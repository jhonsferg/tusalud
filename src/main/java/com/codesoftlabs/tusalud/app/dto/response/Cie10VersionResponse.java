package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cie10Version;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenCie10;
import java.time.LocalDateTime;
import java.util.UUID;

public record Cie10VersionResponse(
        UUID id,
        String etiqueta,
        OrigenCie10 origen,
        int total,
        int altas,
        int cambios,
        String creadaPor,
        LocalDateTime creadaEn,
        boolean activa) {

    public static Cie10VersionResponse from(Cie10Version v) {
        return new Cie10VersionResponse(
                v.getId(),
                v.getEtiqueta(),
                v.getOrigen(),
                v.getTotal(),
                v.getAltas(),
                v.getCambios(),
                v.getCreadaPor() == null ? null : v.getCreadaPor().getNombreCompleto(),
                v.getCreadaEn(),
                v.isActiva());
    }
}
