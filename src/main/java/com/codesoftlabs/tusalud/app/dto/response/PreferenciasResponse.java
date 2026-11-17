package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.PreferenciaUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferencias de notificaciones y recordatorios del usuario")
public record PreferenciasResponse(
        @Schema(example = "true") boolean recordatoriosActivos,

        @Schema(example = "1440") int minutosAntelacionRecordatorio,

        @Schema(example = "true") boolean alertasInicioSesion,

        @Schema(example = "false") boolean tourCompletado) {

    public static final int MINUTOS_POR_DEFECTO = 1440;

    public static PreferenciasResponse porDefecto() {
        return new PreferenciasResponse(true, MINUTOS_POR_DEFECTO, true, false);
    }

    public static PreferenciasResponse from(PreferenciaUsuario preferencia) {
        return new PreferenciasResponse(
                preferencia.isRecordatoriosActivos(),
                preferencia.getMinutosAntelacionRecordatorio(),
                preferencia.isAlertasInicioSesion(),
                preferencia.isTourCompletado());
    }
}
