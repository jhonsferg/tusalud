package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.ExamenLab;
import com.codesoftlabs.tusalud.app.entities.ParametroExamen;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMuestra;
import com.codesoftlabs.tusalud.app.entities.enums.TipoParametro;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Examen del catálogo de laboratorio con sus parámetros")
public record ExamenLabResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "HEM-01") String codigo,
        @Schema(example = "Hemograma completo") String nombre,
        @Schema(example = "HEMATOLOGIA") CategoriaExamen categoria,
        @Schema(example = "SANGRE") TipoMuestra muestra,
        @Schema(example = "35.00") BigDecimal precio,
        @Schema(example = "6") int tiempoEntregaHoras,
        String indicaciones,
        boolean activo,
        List<ParametroResponse> parametros) {

    public record ParametroResponse(
            UUID id,
            String nombre,
            String unidad,
            TipoParametro tipo,
            BigDecimal valorMin,
            BigDecimal valorMax,
            int orden) {

        public static ParametroResponse from(ParametroExamen parametro) {
            return new ParametroResponse(
                    parametro.getId(),
                    parametro.getNombre(),
                    parametro.getUnidad(),
                    parametro.getTipo(),
                    parametro.getValorMin(),
                    parametro.getValorMax(),
                    parametro.getOrden());
        }
    }

    public static ExamenLabResponse from(ExamenLab examen) {
        return new ExamenLabResponse(
                examen.getId(),
                examen.getCodigo(),
                examen.getNombre(),
                examen.getCategoria(),
                examen.getMuestra(),
                examen.getPrecio(),
                examen.getTiempoEntregaHoras(),
                examen.getIndicaciones(),
                examen.isActivo(),
                examen.getParametros().stream().map(ParametroResponse::from).toList());
    }
}
