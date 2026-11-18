package com.codesoftlabs.tusalud.app.storage;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

public interface StorageService {

    record MetadatosObjeto(long tamano, String tipoContenido) {}

    record ObjetoAlmacenado(InputStream flujo, long longitud, String tipoContenido) implements Closeable {

        @Override
        public void close() throws IOException {
            flujo.close();
        }
    }

    void guardar(String bucket, String clave, InputStream contenido, long tamano, String tipoContenido);

    ObjetoAlmacenado abrir(String bucket, String clave);

    ObjetoAlmacenado abrir(String bucket, String clave, long desde, long hasta);

    void eliminar(String bucket, String clave);

    boolean existe(String bucket, String clave);

    Optional<MetadatosObjeto> metadatos(String bucket, String clave);

    Optional<URI> urlPrefirmada(String bucket, String clave, Duration vigencia);
}
