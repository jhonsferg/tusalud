package com.codesoftlabs.tusalud.app.documentos;

public interface ProveedorPlantillas {

    String CLAVE_DEFECTO = "defecto";

    String claveActiva(String codigo);

    byte[] contenido(String codigo);

    byte[] contenidoPorDefecto(String codigo);
}
