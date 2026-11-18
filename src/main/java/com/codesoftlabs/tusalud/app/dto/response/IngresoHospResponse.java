package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cama;
import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.TipoIngresoHosp;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ingreso hospitalario o de emergencia")
public record IngresoHospResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "HOS-000001") String codigo,
        @Schema(example = "PROGRAMADO") TipoIngresoHosp tipo,
        @Schema(example = "ACTIVO") EstadoIngresoHosp estado,
        UUID pacienteId,
        String pacienteNombre,
        String dni,
        UUID camaId,
        String camaCodigo,
        UUID medicoId,
        String medicoNombre,
        String motivo,
        String diagnosticoIngreso,
        LocalDateTime fechaIngreso,
        LocalDateTime fechaAlta,
        String diagnosticoAlta,
        String indicacionesAlta,
        @Schema(example = "3") int diasEstancia,
        @Schema(example = "540.00") BigDecimal cargoCama,
        TriajeResponse triaje,

        @Schema(example = "25", description = "Solo en emergencias que aún esperan una cama")
        Long esperaMinutos,

        @Schema(description = "Motivo de la cancelación, solo en ingresos cancelados y para personal clínico")
        String motivoCancelacion) {

    public IngresoHospResponse sinDatosClinicos() {
        return new IngresoHospResponse(
                id,
                codigo,
                tipo,
                estado,
                pacienteId,
                pacienteNombre,
                dni,
                camaId,
                camaCodigo,
                medicoId,
                medicoNombre,
                null,
                null,
                fechaIngreso,
                fechaAlta,
                null,
                null,
                diasEstancia,
                cargoCama,
                triaje == null ? null : triaje.soloPrioridad(),
                esperaMinutos,
                null);
    }

    public static IngresoHospResponse from(IngresoHospitalario ingreso, LocalDateTime ahora) {
        Cama cama = ingreso.getCama();
        boolean activo = ingreso.getEstado() == EstadoIngresoHosp.ACTIVO;
        LocalDateTime hasta = ingreso.getFechaAlta() != null ? ingreso.getFechaAlta() : ahora;
        BigDecimal cargo = ingreso.getCargoAcumulado();
        if (activo && cama != null && ingreso.getDesdeEnCama() != null) {
            cargo = cargo.add(cama.getPrecioDia().multiply(BigDecimal.valueOf(dias(ingreso.getDesdeEnCama(), ahora))));
        }
        Long espera = activo && ingreso.getTipo() == TipoIngresoHosp.EMERGENCIA && cama == null
                ? Math.max(0, Duration.between(ingreso.getFechaIngreso(), ahora).toMinutes())
                : null;
        return new IngresoHospResponse(
                ingreso.getId(),
                ingreso.getCodigo(),
                ingreso.getTipo(),
                ingreso.getEstado(),
                ingreso.getPaciente().getId(),
                ingreso.getPaciente().getUsuario().getNombreCompleto(),
                ingreso.getPaciente().getDni(),
                cama == null ? null : cama.getId(),
                cama == null ? null : cama.getCodigo(),
                ingreso.getMedico() == null ? null : ingreso.getMedico().getId(),
                ingreso.getMedico() == null
                        ? null
                        : ingreso.getMedico().getUsuario().getNombreCompleto(),
                ingreso.getMotivo(),
                ingreso.getDiagnosticoIngreso(),
                ingreso.getFechaIngreso(),
                ingreso.getFechaAlta(),
                ingreso.getDiagnosticoAlta(),
                ingreso.getIndicacionesAlta(),
                (int) dias(ingreso.getFechaIngreso(), hasta),
                cargo,
                TriajeResponse.from(ingreso),
                espera,
                ingreso.getEstado() == EstadoIngresoHosp.CANCELADO ? ingreso.getMotivoCancelacion() : null);
    }

    public static long dias(LocalDateTime desde, LocalDateTime hasta) {
        long minutos = Math.max(0, Duration.between(desde, hasta).toMinutes());
        return Math.max(1, (minutos + 1439) / 1440);
    }
}
