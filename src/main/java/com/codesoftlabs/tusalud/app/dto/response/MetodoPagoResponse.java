package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.MetodoPago;
import com.codesoftlabs.tusalud.app.entities.enums.MarcaTarjeta;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.YearMonth;

@Schema(description = "Método de pago guardado (sin número completo ni CVV)")
public record MetodoPagoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "TARJETA_CREDITO") TipoMetodoPago tipo,
        @Schema(example = "JUAN PEREZ") String titular,
        @Schema(example = "VISA") MarcaTarjeta marca,
        @Schema(example = "4242") String ultimos4,
        @Schema(example = "12") Integer vencimientoMes,
        @Schema(example = "2030") Integer vencimientoAnio,
        @Schema(example = "true") boolean predeterminado,
        @Schema(example = "false") boolean vencida) {

    public static MetodoPagoResponse from(MetodoPago metodo, YearMonth hoy) {
        return new MetodoPagoResponse(
                metodo.getId(),
                metodo.getTipo(),
                metodo.getTitular(),
                metodo.getMarca(),
                metodo.getUltimos4(),
                metodo.getVencimientoMes(),
                metodo.getVencimientoAnio(),
                metodo.isPredeterminado(),
                metodo.estaVencida(hoy));
    }
}
