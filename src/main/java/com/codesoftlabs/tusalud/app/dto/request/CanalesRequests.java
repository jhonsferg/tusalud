package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.AlcanceRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.CategoriaRetencion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.TipoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class CanalesRequests {

    private CanalesRequests() {}

    public record SolicitudContactoRequest(@Size(max = 150) String destino, boolean consentimiento) {}

    public record VerificacionContactoRequest(
            @NotBlank @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos")
            String codigo) {}

    public record PreferenciaCanalRequest(
            @NotNull TipoNotificacion tipo, @NotNull Canal canal, boolean activo) {}

    public record SilencioRequest(
            boolean activo,
            @NotNull LocalTime desde,
            @NotNull LocalTime hasta) {}

    public record PreferenciasCanalesRequest(
            @Valid @Size(max = 200) List<PreferenciaCanalRequest> preferencias,
            @Valid SilencioRequest silencio) {}

    public record PlantillaMensajeRequest(
            @Size(max = 150) String asunto,
            @NotBlank @Size(max = 1000) String cuerpo,
            @NotBlank @Size(min = 5, max = 200) String motivo) {}

    public record EstadoEntregaWebhookRequest(
            @NotBlank @Size(max = 100) String idExterno,
            @NotNull EstadoEntrega estado,
            @Size(max = 200) String motivo) {}

    public record RespaldoEjecucionRequest(
            @NotNull TipoEjecucionRespaldo tipo,
            @NotNull EstadoEjecucionRespaldo estado,
            @NotNull AlcanceRespaldo alcance,
            CategoriaRetencion categoria,
            @NotNull LocalDateTime iniciadoEn,
            @NotNull LocalDateTime finalizadoEn,
            @PositiveOrZero Long bytesTotal,
            @Size(max = 200) String archivo,
            @Size(max = 64) String checksum,
            @PositiveOrZero Integer migracionesValidadas,
            @PositiveOrZero Integer tablasValidadas,
            @PositiveOrZero Integer objetosAlmacen,
            boolean copiaRemota,
            @Size(max = 500) String detalle) {}
}
