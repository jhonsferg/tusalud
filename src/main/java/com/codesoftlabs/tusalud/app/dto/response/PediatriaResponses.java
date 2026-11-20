package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoDosis;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoRegistroVacunacion;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.IndicadorCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.NivelAlerta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class PediatriaResponses {

    private PediatriaResponses() {}

    public record VacunaResponse(UUID id, String codigo, String nombre, String descripcion) {}

    public record EsquemaDosisResponse(
            UUID vacunaId,
            String vacunaCodigo,
            String vacunaNombre,
            int numeroDosis,
            String etiqueta,
            int edadMinimaDias,
            int edadRecomendadaDias,
            Integer edadMaximaDias,
            Integer intervaloMinimoDias) {}

    public record EsquemaResponse(
            UUID id,
            String nombre,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta,
            boolean porValidar,
            boolean activo,
            String fuente,
            List<EsquemaDosisResponse> dosis) {}

    public record DosisCarne(
            UUID vacunaId,
            String vacunaCodigo,
            String vacunaNombre,
            int numeroDosis,
            String etiqueta,
            EstadoDosis estado,
            LocalDate fechaRecomendada,
            LocalDate fechaAplicada,
            Integer atrasoDias,
            UUID registroId) {}

    public record RegistroVacunaResponse(
            UUID id,
            UUID vacunaId,
            String vacunaNombre,
            int numeroDosis,
            LocalDate fechaAplicacion,
            String lote,
            String establecimiento,
            String responsableNombre,
            EstadoRegistroVacunacion estado,
            String observaciones,
            String motivoAnulacion) {}

    public record CarneResponse(
            UUID pacienteId,
            String pacienteNombre,
            String dni,
            LocalDate fechaNacimiento,
            Integer edadMeses,
            String sexo,
            String esquemaNombre,
            boolean esquemaPorValidar,
            int aplicadas,
            int atrasadas,
            int proximas,
            DosisCarne proximaDosis,
            List<DosisCarne> dosis,
            List<RegistroVacunaResponse> registros) {}

    public record PacienteClinicoResumen(
            UUID id, String nombre, String dni, LocalDate fechaNacimiento, Integer edadMeses, String sexo) {}

    public record MedicionResponse(
            UUID id,
            LocalDate fecha,
            BigDecimal pesoKg,
            BigDecimal tallaCm,
            BigDecimal perimetroCefalicoCm,
            BigDecimal imc,
            String registradoPor,
            String observaciones) {}

    public record PuntoCrecimiento(
            UUID medicionId,
            LocalDate fecha,
            double edadMeses,
            double valor,
            Double z,
            Double percentil,
            NivelAlerta nivel,
            String mensaje) {}

    public record ReferenciaPunto(int edadMeses, double p3, double p15, double p50, double p85, double p97) {}

    public record CurvaCrecimiento(
            IndicadorCrecimiento indicador,
            String etiqueta,
            String unidad,
            List<PuntoCrecimiento> puntos,
            List<ReferenciaPunto> referencia) {}

    public record AlertaCrecimiento(
            IndicadorCrecimiento indicador,
            String etiqueta,
            NivelAlerta nivel,
            String mensaje,
            double z,
            LocalDate fecha) {}

    public record CrecimientoResponse(
            UUID pacienteId,
            String pacienteNombre,
            String sexo,
            Integer edadMeses,
            boolean aplica,
            boolean referenciaPorValidar,
            List<CurvaCrecimiento> curvas,
            List<AlertaCrecimiento> alertas,
            List<MedicionResponse> mediciones) {}

    public record CarneEmitidoResponse(String codigoVerificacion, LocalDateTime emitidoEn) {}
}
