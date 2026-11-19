package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.Afectacion;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ModoFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FacturacionRequests {

    private FacturacionRequests() {}

    public record ConfiguracionFiscalRequest(
            boolean habilitada,

            @NotBlank @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
            String ruc,

            @NotBlank @Size(max = 150) String razonSocial,
            @Size(max = 150) String nombreComercial,
            @NotBlank @Size(max = 200) String direccion,

            @NotBlank @Pattern(regexp = "\\d{6}", message = "El ubigeo debe tener 6 dígitos")
            String ubigeo,

            @NotBlank @Size(max = 60) String departamento,
            @NotBlank @Size(max = 60) String provincia,
            @NotBlank @Size(max = 60) String distrito,

            @Pattern(regexp = "\\d{4}", message = "El código de local debe tener 4 dígitos")
            String codigoLocal,

            @NotNull ProveedorFiscal proveedor,
            @NotNull ModoFiscal modo,
            @Size(max = 300) String urlServicio,
            @NotNull Afectacion afectacionServicios,
            boolean boletasPorResumen,
            @Min(1) @Max(7) Integer diasLimiteBaja) {}

    public record SerieRequest(
            @NotNull TipoCpe tipo,

            @NotBlank
            @Pattern(regexp = "[FB][A-Z0-9]{3}", message = "La serie debe empezar con F o B y tener 4 caracteres")
            String serie,

            @Min(0) Integer correlativoInicial) {}

    public record SerieEstadoRequest(boolean activa) {}

    public record NotaItemRequest(
            @NotBlank @Size(max = 200) String descripcion,
            @NotNull @Positive BigDecimal cantidad,
            @NotNull @Positive BigDecimal precioUnitario,
            Afectacion afectacion) {}

    public record NotaRequest(
            @NotNull UUID documentoId,
            @NotNull TipoCpe tipo,
            @NotBlank @Pattern(regexp = "\\d{2}") String motivoCodigo,
            @NotBlank @Size(min = 5, max = 250) String descripcion,
            @Valid List<NotaItemRequest> items) {}

    public record MotivoBajaRequest(
            @NotBlank @Size(min = 5, max = 250) String motivo) {}

    public record CorreccionClienteRequest(
            @NotNull TipoDocumentoCliente tipoDoc,
            @Size(max = 11) String documento,
            @NotBlank @Size(max = 150) String nombre,
            @Size(max = 200) String direccion) {}

    public record ResumenDiarioRequest(@NotNull LocalDate fecha) {}

    public record ItemsNotaRequest(@NotEmpty @Valid List<NotaItemRequest> items) {}
}
