package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronico;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronicoItem;
import com.codesoftlabs.tusalud.app.entities.IntercambioSunat;
import com.codesoftlabs.tusalud.app.entities.ResumenSunat;
import com.codesoftlabs.tusalud.app.entities.SerieComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.Afectacion;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoResumen;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ModoFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.OperacionSunat;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoResumen;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FacturacionResponses {

    private FacturacionResponses() {}

    public record ConfiguracionFiscalResponse(
            UUID id,
            String sede,
            boolean habilitada,
            String ruc,
            String razonSocial,
            String nombreComercial,
            String direccion,
            String ubigeo,
            String departamento,
            String provincia,
            String distrito,
            String codigoLocal,
            ProveedorFiscal proveedor,
            ModoFiscal modo,
            String urlServicio,
            Afectacion afectacionServicios,
            BigDecimal igvPorcentaje,
            boolean boletasPorResumen,
            int diasLimiteBaja,
            boolean certificadoDePrueba,
            boolean credencialesSolConfiguradas,
            LocalDateTime actualizadoEn) {

        public static ConfiguracionFiscalResponse de(
                ConfiguracionFiscal c, boolean certificadoDePrueba, boolean credencialesSol) {
            return new ConfiguracionFiscalResponse(
                    c.getId(),
                    c.getSede(),
                    c.isHabilitada(),
                    c.getRuc(),
                    c.getRazonSocial(),
                    c.getNombreComercial(),
                    c.getDireccion(),
                    c.getUbigeo(),
                    c.getDepartamento(),
                    c.getProvincia(),
                    c.getDistrito(),
                    c.getCodigoLocal(),
                    c.getProveedor(),
                    c.getModo(),
                    c.getUrlServicio(),
                    c.getAfectacionServicios(),
                    c.getIgvPorcentaje(),
                    c.isBoletasPorResumen(),
                    c.getDiasLimiteBaja(),
                    certificadoDePrueba,
                    credencialesSol,
                    c.getActualizadoEn());
        }
    }

    public record SerieResponse(
            UUID id, String sede, TipoCpe tipo, String serie, int correlativoActual, boolean activa) {

        public static SerieResponse de(SerieComprobante s) {
            return new SerieResponse(
                    s.getId(), s.getSede(), s.getTipo(), s.getSerie(), s.getCorrelativoActual(), s.isActiva());
        }
    }

    public record ItemCpeResponse(
            int posicion,
            String codigo,
            String descripcion,
            String unidad,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal valorVenta,
            Afectacion afectacion,
            BigDecimal igv,
            BigDecimal total) {

        public static ItemCpeResponse de(DocumentoElectronicoItem i) {
            return new ItemCpeResponse(
                    i.getPosicion(),
                    i.getCodigo(),
                    i.getDescripcion(),
                    i.getUnidad(),
                    i.getCantidad(),
                    i.getPrecioUnitario(),
                    i.getValorVenta(),
                    i.getAfectacion(),
                    i.getIgv(),
                    i.getTotal());
        }
    }

    public record IntercambioResponse(
            UUID id,
            OperacionSunat operacion,
            ProveedorFiscal proveedor,
            boolean exitoso,
            String codigoRespuesta,
            String mensaje,
            int duracionMs,
            LocalDateTime creadoEn) {

        public static IntercambioResponse de(IntercambioSunat i) {
            return new IntercambioResponse(
                    i.getId(),
                    i.getOperacion(),
                    i.getProveedor(),
                    i.isExitoso(),
                    i.getCodigoRespuesta(),
                    i.getMensaje(),
                    i.getDuracionMs(),
                    i.getCreadoEn());
        }
    }

    public record DocumentoElectronicoResumen(
            UUID id,
            TipoCpe tipo,
            String serie,
            Integer numero,
            String numeroCompleto,
            LocalDateTime emitidoEn,
            TipoDocumentoCliente clienteTipoDoc,
            String clienteDoc,
            String clienteNombre,
            BigDecimal total,
            EstadoCpe estado,
            String cdrDescripcion,
            int intentos,
            UUID comprobanteId,
            UUID documentoReferenciaId) {

        public static DocumentoElectronicoResumen de(DocumentoElectronico d) {
            return new DocumentoElectronicoResumen(
                    d.getId(),
                    d.getTipo(),
                    d.getSerie(),
                    d.getNumero(),
                    d.numeroCompleto(),
                    d.getEmitidoEn(),
                    d.getClienteTipoDoc(),
                    d.getClienteDoc(),
                    d.getClienteNombre(),
                    d.getTotal(),
                    d.getEstado(),
                    d.getCdrDescripcion(),
                    d.getIntentos(),
                    d.getComprobante() == null ? null : d.getComprobante().getId(),
                    d.getDocumentoReferencia() == null
                            ? null
                            : d.getDocumentoReferencia().getId());
        }
    }

    public record DocumentoElectronicoDetalle(
            UUID id,
            TipoCpe tipo,
            String serie,
            Integer numero,
            String numeroCompleto,
            LocalDateTime emitidoEn,
            String moneda,
            TipoDocumentoCliente clienteTipoDoc,
            String clienteDoc,
            String clienteNombre,
            String clienteDireccion,
            BigDecimal opGravada,
            BigDecimal opExonerada,
            BigDecimal opInafecta,
            BigDecimal igv,
            BigDecimal total,
            EstadoCpe estado,
            String motivoCodigo,
            String motivoDescripcion,
            String digestValue,
            String xmlSha256,
            String cdrCodigo,
            String cdrDescripcion,
            LocalDateTime cdrRecibidoEn,
            int intentos,
            LocalDateTime proximoIntento,
            String ultimoError,
            String motivoBaja,
            UUID comprobanteId,
            UUID documentoReferenciaId,
            String documentoReferenciaNumero,
            BigDecimal acreditado,
            List<ItemCpeResponse> items,
            List<IntercambioResponse> intercambios) {}

    public record ResumenSunatResponse(
            UUID id,
            TipoResumen tipo,
            String identificador,
            LocalDate fechaReferencia,
            LocalDate fechaGeneracion,
            EstadoResumen estado,
            String ticket,
            String cdrCodigo,
            String cdrDescripcion,
            String motivo,
            int intentos,
            String ultimoError,
            int documentos,
            LocalDateTime creadoEn) {

        public static ResumenSunatResponse de(ResumenSunat r, int documentos) {
            return new ResumenSunatResponse(
                    r.getId(),
                    r.getTipo(),
                    r.getIdentificador(),
                    r.getFechaReferencia(),
                    r.getFechaGeneracion(),
                    r.getEstado(),
                    r.getTicket(),
                    r.getCdrCodigo(),
                    r.getCdrDescripcion(),
                    r.getMotivo(),
                    r.getIntentos(),
                    r.getUltimoError(),
                    documentos,
                    r.getCreadoEn());
        }
    }

    public record FilaRegistroVentas(
            LocalDate fecha,
            String tipo,
            String serie,
            Integer numero,
            String clienteTipoDoc,
            String clienteDoc,
            String clienteNombre,
            BigDecimal gravada,
            BigDecimal exonerada,
            BigDecimal inafecta,
            BigDecimal igv,
            BigDecimal total,
            EstadoCpe estado) {}

    public record RegistroVentasResponse(
            LocalDate desde,
            LocalDate hasta,
            List<FilaRegistroVentas> filas,
            BigDecimal totalGravada,
            BigDecimal totalExonerada,
            BigDecimal totalInafecta,
            BigDecimal totalIgv,
            BigDecimal total) {}

    public record EstadoFacturacionResponse(
            boolean habilitada,
            ProveedorFiscal proveedor,
            ModoFiscal modo,
            boolean certificadoDePrueba,
            Map<EstadoCpe, Long> conteo,
            long pendientesDeEnvio,
            LocalDateTime ultimoIntercambio) {}

    public record MotivoNotaResponse(String codigo, String descripcion, boolean total, TipoCpe tipo) {}
}
