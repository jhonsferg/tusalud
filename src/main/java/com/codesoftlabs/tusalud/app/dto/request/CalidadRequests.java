package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.MetodoAnalisis;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.SeveridadEvento;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.TipoEvento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CalidadRequests {

    private CalidadRequests() {}

    public record EventoRequest(
            @NotNull TipoEvento tipo,
            @NotNull SeveridadEvento severidad,
            @NotBlank @Size(max = 60) String area,
            @NotBlank @Size(min = 10, max = 2000) String descripcion,
            boolean anonimo,
            @NotNull LocalDateTime ocurridoEn,
            UUID pacienteId,
            UUID cirugiaId,
            UUID dispensacionId) {}

    public record CausaRequest(
            @NotBlank @Size(max = 30) String categoria,
            @NotBlank @Size(max = 500) String texto) {}

    public record AnalisisRequest(
            @NotNull MetodoAnalisis metodo,
            @NotEmpty @Size(max = 12) List<@Valid CausaRequest> causas,
            @NotBlank @Size(max = 1000) String conclusion,
            @Size(max = 40) String clasificacion) {}

    public record AccionRequest(
            @NotBlank @Size(max = 500) String descripcion,
            @NotNull UUID responsableId,
            @NotNull LocalDate fechaLimite) {}

    public record CierreEventoRequest(
            @NotBlank @Size(max = 1000) String verificacion,
            @NotNull Boolean accionEficaz) {}

    public record GuiaRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 200) String titulo,
            UUID especialidadId,
            @NotBlank @Size(max = 1000) String resumen,
            @NotBlank @Size(max = 20000) String contenido,
            @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 10) String> codigosCie10,
            @NotNull LocalDate vigenteDesde,
            boolean porValidar) {}
}
