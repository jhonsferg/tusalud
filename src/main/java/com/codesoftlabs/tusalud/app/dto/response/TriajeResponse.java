package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.enums.ClasificacionTriaje;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Triaje registrado al llegar a emergencia")
public record TriajeResponse(
        @Schema(example = "2") int nivel,
        @Schema(example = "NARANJA") ClasificacionTriaje clasificacion,
        String presionArterial,
        Integer frecuenciaCardiaca,
        Integer frecuenciaRespiratoria,
        BigDecimal temperatura,
        Integer saturacionO2,
        Integer glasgow,
        String motivoConsulta,
        String registradoPor,
        LocalDateTime fecha) {

    public TriajeResponse soloPrioridad() {
        return new TriajeResponse(nivel, clasificacion, null, null, null, null, null, null, null, null, fecha);
    }

    public static TriajeResponse from(IngresoHospitalario ingreso) {
        if (!ingreso.tieneTriaje()) {
            return null;
        }
        return new TriajeResponse(
                ingreso.getTriajeNivel(),
                ClasificacionTriaje.deNivel(ingreso.getTriajeNivel()),
                ingreso.getTriajePresionArterial(),
                ingreso.getTriajeFrecuenciaCardiaca(),
                ingreso.getTriajeFrecuenciaRespiratoria(),
                ingreso.getTriajeTemperatura(),
                ingreso.getTriajeSaturacionO2(),
                ingreso.getTriajeGlasgow(),
                ingreso.getTriajeMotivoConsulta(),
                ingreso.getTriajeRegistradoPor() == null
                        ? null
                        : ingreso.getTriajeRegistradoPor().getNombreCompleto(),
                ingreso.getTriajeFecha());
    }
}
