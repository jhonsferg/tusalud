package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.repositories.ArchivoImagenRepository.Metadatos;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArchivoImagenResponse(UUID id, String nombre, String tipoContenido, long tamano, LocalDateTime subidoEn) {

    public static ArchivoImagenResponse from(Metadatos metadatos) {
        return new ArchivoImagenResponse(
                metadatos.id(),
                metadatos.nombre(),
                metadatos.tipoContenido(),
                metadatos.tamano(),
                metadatos.subidoEn());
    }
}
