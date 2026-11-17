package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.ItemOrdenLab;
import com.codesoftlabs.tusalud.app.entities.OrdenLaboratorio;
import com.codesoftlabs.tusalud.app.entities.ResultadoLab;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoItemLab;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMuestra;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Orden de laboratorio con sus exámenes y resultados")
public record OrdenLabResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "LAB-000001") String codigo,

        @Schema(type = "string", example = "2026-11-17T10:00:00")
        LocalDateTime fecha,

        @Schema(example = "SOLICITADA") EstadoOrdenLab estado,
        @Schema(example = "NORMAL") PrioridadOrden prioridad,
        Long pacienteId,
        String pacienteNombre,
        String dni,
        Long medicoId,
        String medicoNombre,
        Long atencionId,
        String indicaciones,
        BigDecimal total,

        @Schema(type = "string", example = "2026-11-17T11:00:00")
        LocalDateTime tomaMuestraEn,

        @Schema(type = "string", example = "2026-11-17T15:00:00")
        LocalDateTime completadaEn,

        List<ItemResponse> items) {

    public record ItemResponse(
            Long id,
            Long examenId,
            String examenNombre,
            CategoriaExamen categoria,
            TipoMuestra muestra,
            EstadoItemLab estado,
            String observaciones,
            LocalDateTime fechaResultado,
            String validadoPor,
            List<ResultadoResponse> resultados) {

        public static ItemResponse from(ItemOrdenLab item, boolean incluirResultados) {
            return new ItemResponse(
                    item.getId(),
                    item.getExamen().getId(),
                    item.getExamen().getNombre(),
                    item.getExamen().getCategoria(),
                    item.getExamen().getMuestra(),
                    item.getEstado(),
                    incluirResultados ? item.getObservaciones() : null,
                    incluirResultados ? item.getFechaResultado() : null,
                    incluirResultados && item.getValidadoPor() != null
                            ? item.getValidadoPor().getNombreCompleto()
                            : null,
                    incluirResultados
                            ? item.getResultados().stream()
                                    .map(ResultadoResponse::from)
                                    .toList()
                            : List.of());
        }
    }

    public record ResultadoResponse(
            Long parametroId,
            String parametro,
            String unidad,
            String valor,
            BigDecimal valorMin,
            BigDecimal valorMax,
            boolean fueraDeRango) {

        public static ResultadoResponse from(ResultadoLab resultado) {
            return new ResultadoResponse(
                    resultado.getParametroId(),
                    resultado.getParametroNombre(),
                    resultado.getUnidad(),
                    resultado.getValor(),
                    resultado.getValorMin(),
                    resultado.getValorMax(),
                    resultado.isFueraDeRango());
        }
    }

    public static OrdenLabResponse from(OrdenLaboratorio orden, boolean incluirResultados) {
        return new OrdenLabResponse(
                orden.getId(),
                orden.getCodigo(),
                orden.getFecha(),
                orden.getEstado(),
                orden.getPrioridad(),
                orden.getPaciente().getId(),
                orden.getPaciente().getUsuario().getNombreCompleto(),
                orden.getPaciente().getDni(),
                orden.getMedico().getId(),
                orden.getMedico().getUsuario().getNombreCompleto(),
                orden.getAtencion() == null ? null : orden.getAtencion().getId(),
                orden.getIndicaciones(),
                orden.getTotal(),
                orden.getTomaMuestraEn(),
                orden.getCompletadaEn(),
                orden.getItems().stream()
                        .map(item -> ItemResponse.from(item, incluirResultados))
                        .toList());
    }
}
