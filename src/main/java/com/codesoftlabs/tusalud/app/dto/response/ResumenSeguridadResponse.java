package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;

public record ResumenSeguridadResponse(
        VentanaSeguridad ultimas24Horas,
        VentanaSeguridad ultimos7Dias,
        List<EventoSeguridadResponse> iniciosInusuales) {

    public record VentanaSeguridad(long total, long altas, long avisos, List<ConteoTipo> porTipo) {}

    public record ConteoTipo(String tipo, String etiqueta, String severidad, long cantidad) {}
}
