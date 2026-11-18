package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArchivoResponse(
        UUID id,
        String nombre,
        String tipoContenido,
        long tamano,
        String sha256,
        PropositoArchivo proposito,
        String referenciaTipo,
        UUID referenciaId,
        LocalDateTime createdAt) {

    public static ArchivoResponse from(Archivo archivo) {
        return new ArchivoResponse(
                archivo.getId(),
                archivo.getNombreOriginal(),
                archivo.getTipoContenido(),
                archivo.getTamano(),
                archivo.getSha256(),
                archivo.getProposito(),
                archivo.getReferenciaTipo(),
                archivo.getReferenciaId(),
                archivo.getCreatedAt());
    }
}
