package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;

public record Cie10ImportacionResponse(
        boolean aplicada,
        int filas,
        int validas,
        int altas,
        int cambios,
        int sinCambios,
        int desactivados,
        int totalErrores,
        List<Error> errores,
        List<Fila> muestra) {

    public record Error(int fila, String mensaje) {}

    public record Fila(
            String codigo,
            String descripcion,
            String capitulo,
            String grupo,
            String sexo,
            Integer edadMinima,
            Integer edadMaxima,
            String accion) {}
}
