package com.codesoftlabs.tusalud.app.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record EstadoSistemaResponse(
        String estadoGeneral,
        String version,
        LocalDateTime iniciadoEn,
        long segundosEnMarcha,
        List<ComponenteEstado> componentes,
        Migraciones migraciones) {

    public record ComponenteEstado(String nombre, String estado, Long latenciaMs, String detalle) {}

    public record Migraciones(boolean disponible, long aplicadas, String ultima) {}
}
