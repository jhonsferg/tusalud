package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.ArchivoResponse;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import com.codesoftlabs.tusalud.app.storage.StorageService.ObjetoAlmacenado;
import java.util.List;
import java.util.UUID;

public interface ArchivoService {

    String REFERENCIA_PACIENTE = "PACIENTE";
    String REFERENCIA_ORDEN_IMAGEN = "ORDEN_IMAGEN";

    record Descarga(Archivo archivo, ObjetoAlmacenado objeto, long desde, long hasta, boolean parcial) {

        public long longitud() {
            return hasta - desde + 1;
        }
    }

    ArchivoResponse subir(
            UsuarioPrincipal actor,
            PropositoArchivo proposito,
            String nombre,
            byte[] contenido,
            UUID referenciaId,
            String referenciaTipo);

    Archivo almacenar(
            Usuario subidoPor,
            Usuario propietario,
            PropositoArchivo proposito,
            String nombre,
            byte[] contenido,
            String referenciaTipo,
            UUID referenciaId);

    Descarga descargar(UsuarioPrincipal actor, UUID id, String rango);

    Descarga abrirSinAutorizar(Archivo archivo, String rango);

    ArchivoResponse metadatos(UsuarioPrincipal actor, UUID id);

    void eliminar(UsuarioPrincipal actor, UUID id);

    void eliminarSinAutorizar(Archivo archivo);

    List<ArchivoResponse> listar(UsuarioPrincipal actor, String referenciaTipo, UUID referenciaId);
}
