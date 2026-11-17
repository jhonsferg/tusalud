package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.PreferenciaUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferencias de notificaciones y recordatorios del usuario")
public record PreferenciasResponse(
        @Schema(example = "true") boolean recordatoriosActivos,

        @Schema(example = "24") int horasAntelacionRecordatorio,

        @Schema(example = "true") boolean alertasInicioSesion) {

    public static final int HORAS_POR_DEFECTO = 24;

    public static PreferenciasResponse porDefecto() {
        return new PreferenciasResponse(true, HORAS_POR_DEFECTO, true);
    }

    public static PreferenciasResponse from(PreferenciaUsuario preferencia) {
        return new PreferenciasResponse(
                preferencia.isRecordatoriosActivos(),
                preferencia.getHorasAntelacionRecordatorio(),
                preferencia.isAlertasInicioSesion());
    }
}
