package com.codesoftlabs.tusalud.app.facturacion;

import com.codesoftlabs.tusalud.app.storage.StorageProperties;
import com.codesoftlabs.tusalud.app.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AlmacenFiscal {

    private final StorageService almacenamiento;
    private final StorageProperties propiedades;

    public AlmacenFiscal(StorageService almacenamiento, StorageProperties propiedades) {
        this.almacenamiento = almacenamiento;
        this.propiedades = propiedades;
    }

    public String guardarXml(String carpeta, UUID id, byte[] contenido) {
        return guardar(clave("fiscal", carpeta, id, "xml"), contenido, "application/xml");
    }

    public String guardarZip(String carpeta, UUID id, byte[] contenido) {
        return guardar(clave("fiscal", carpeta, id, "zip"), contenido, "application/zip");
    }

    public String guardarJson(String carpeta, UUID id, byte[] contenido) {
        return guardar(clave(null, carpeta, id, "json"), contenido, "application/json");
    }

    public byte[] leer(String clave) {
        try (var objeto = almacenamiento.abrir(propiedades.buckets().documentos(), clave)) {
            return objeto.flujo().readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el objeto almacenado", ex);
        }
    }

    public void eliminar(String clave) {
        almacenamiento.eliminar(propiedades.buckets().documentos(), clave);
    }

    private static String clave(String prefijo, String carpeta, UUID id, String extension) {
        String base = (prefijo == null ? "" : prefijo + "-") + carpeta.replace('/', '-');
        return base + "-" + id + "." + extension;
    }

    private String guardar(String clave, byte[] contenido, String mime) {
        almacenamiento.guardar(
                propiedades.buckets().documentos(), clave, new ByteArrayInputStream(contenido), contenido.length, mime);
        return clave;
    }
}
