package com.codesoftlabs.tusalud.app.services.facturacion;

import com.codesoftlabs.tusalud.app.config.FacturacionProperties;
import com.codesoftlabs.tusalud.app.documentos.Etiquetas;
import com.codesoftlabs.tusalud.app.documentos.FichaDocumento;
import com.codesoftlabs.tusalud.app.documentos.Formato;
import com.codesoftlabs.tusalud.app.documentos.HojaExcel;
import com.codesoftlabs.tusalud.app.documentos.LibroExcel;
import com.codesoftlabs.tusalud.app.documentos.MotorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.QrGenerador;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.FilaRegistroVentas;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.RegistroVentasResponse;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronico;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.facturacion.NumeroALetras;
import com.codesoftlabs.tusalud.app.repositories.DocumentoElectronicoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacturacionDocumentos {

    private static final int DIAS_MAXIMOS_REGISTRO = 366;

    private final FacturacionService facturacion;
    private final DocumentoElectronicoRepository documentos;
    private final MotorPlantillas motor;
    private final FacturacionProperties propiedades;

    public record Archivo(byte[] contenido, String nombre) {}

    public Archivo pdf(UsuarioPrincipal actor, UUID id) {
        DocumentoElectronico d = facturacion.cargar(id);
        autorizar(actor, d);
        ConfiguracionFiscal c = facturacion.configuracionVigente().orElse(null);
        return new Archivo(
                renderizar(d, c, actor.getNombreCompleto()),
                "comprobante-" + d.getSerie() + "-" + d.getNumero() + ".pdf");
    }

    public RegistroVentasResponse registroVentas(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.withDayOfMonth(1) : desde;
        if (inicio.isAfter(fin)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        if (inicio.plusDays(DIAS_MAXIMOS_REGISTRO).isBefore(fin)) {
            throw new DatosInvalidosException("El rango del registro no puede superar un año");
        }
        List<DocumentoElectronico> lista = documentos.paraRegistro(
                inicio.atStartOfDay(),
                fin.plusDays(1).atStartOfDay(),
                EnumSet.of(EstadoCpe.ACEPTADO, EstadoCpe.ACEPTADO_OBSERVACIONES, EstadoCpe.ANULADO));
        List<FilaRegistroVentas> filas = new ArrayList<>();
        BigDecimal gravada = BigDecimal.ZERO;
        BigDecimal exonerada = BigDecimal.ZERO;
        BigDecimal inafecta = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (DocumentoElectronico d : lista) {
            boolean anulado = d.getEstado() == EstadoCpe.ANULADO;
            BigDecimal signo = d.getTipo() == TipoCpe.NOTA_CREDITO ? BigDecimal.ONE.negate() : BigDecimal.ONE;
            BigDecimal g = anulado ? BigDecimal.ZERO : d.getOpGravada().multiply(signo);
            BigDecimal e = anulado ? BigDecimal.ZERO : d.getOpExonerada().multiply(signo);
            BigDecimal i = anulado ? BigDecimal.ZERO : d.getOpInafecta().multiply(signo);
            BigDecimal v = anulado ? BigDecimal.ZERO : d.getIgv().multiply(signo);
            BigDecimal t = anulado ? BigDecimal.ZERO : d.getTotal().multiply(signo);
            filas.add(new FilaRegistroVentas(
                    d.getEmitidoEn().toLocalDate(),
                    d.getTipo().codigo(),
                    d.getSerie(),
                    d.getNumero(),
                    d.getClienteTipoDoc().name(),
                    d.getClienteDoc(),
                    d.getClienteNombre(),
                    g,
                    e,
                    i,
                    v,
                    t,
                    d.getEstado()));
            gravada = gravada.add(g);
            exonerada = exonerada.add(e);
            inafecta = inafecta.add(i);
            igv = igv.add(v);
            total = total.add(t);
        }
        return new RegistroVentasResponse(inicio, fin, filas, gravada, exonerada, inafecta, igv, total);
    }

    public Archivo excelRegistroVentas(UsuarioPrincipal actor, LocalDate desde, LocalDate hasta) {
        RegistroVentasResponse registro = registroVentas(desde, hasta);
        HojaExcel hoja = HojaExcel.de(
                "Registro de ventas",
                "Fecha",
                "Tipo",
                "Serie",
                "Número",
                "Tipo de documento",
                "Documento",
                "Cliente",
                "Gravada",
                "Exonerada",
                "Inafecta",
                "IGV",
                "Total",
                "Estado");
        for (FilaRegistroVentas f : registro.filas()) {
            hoja.fila(
                    f.fecha(),
                    f.tipo(),
                    f.serie(),
                    f.numero(),
                    f.clienteTipoDoc(),
                    f.clienteDoc() == null ? "" : f.clienteDoc(),
                    f.clienteNombre(),
                    f.gravada(),
                    f.exonerada(),
                    f.inafecta(),
                    f.igv(),
                    f.total(),
                    f.estado().name());
        }
        hoja.fila(
                "Totales",
                "",
                "",
                "",
                "",
                "",
                "",
                registro.totalGravada(),
                registro.totalExonerada(),
                registro.totalInafecta(),
                registro.totalIgv(),
                registro.total(),
                "");
        byte[] contenido =
                LibroExcel.construir(actor.getNombreCompleto(), "registro-ventas", "Registro de ventas", List.of(hoja));
        return new Archivo(contenido, "registro-ventas-" + registro.desde() + "-" + registro.hasta() + ".xlsx");
    }

    public String contenidoQr(DocumentoElectronico d, ConfiguracionFiscal c) {
        return String.join(
                "|",
                c == null ? "" : c.getRuc(),
                d.getTipo().codigo(),
                d.getSerie(),
                String.valueOf(d.getNumero()),
                d.getIgv().toPlainString(),
                d.getTotal().toPlainString(),
                d.getEmitidoEn().toLocalDate().toString(),
                switch (d.getClienteTipoDoc()) {
                    case RUC -> "6";
                    case DNI -> "1";
                    case SIN_DOC -> "0";
                },
                d.getClienteDoc() == null ? "-" : d.getClienteDoc(),
                "");
    }

    private byte[] renderizar(DocumentoElectronico d, ConfiguracionFiscal c, String generadoPor) {
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.COMPROBANTE_ELECTRONICO);
        String emisor = c == null ? "" : c.getRazonSocial() + " · RUC " + c.getRuc();
        ficha.titulo(
                d.getTipo().etiqueta().toUpperCase() + " ELECTRÓNICA " + d.getSerie() + "-" + d.getNumero(), emisor);
        ficha.par("Emisión", Formato.fechaHora(d.getEmitidoEn()))
                .par("Cliente", d.getClienteNombre())
                .par(
                        d.getClienteTipoDoc().name().equals("SIN_DOC")
                                ? "Documento"
                                : d.getClienteTipoDoc().name(),
                        d.getClienteDoc() == null ? "Sin documento" : d.getClienteDoc())
                .par("Dirección", Formato.texto(d.getClienteDireccion()))
                .par("Moneda", "Soles (PEN)")
                .par("Estado ante SUNAT", etiquetaEstado(d.getEstado()));
        if (d.getComprobante() != null) {
            ficha.par("Método de pago", Etiquetas.metodoPago(d.getComprobante().getMetodoPago()));
        }
        if (d.getDocumentoReferencia() != null) {
            ficha.par("Documento que modifica", d.getDocumentoReferencia().numeroCompleto())
                    .par("Motivo", d.getMotivoCodigo() + " · " + d.getMotivoDescripcion());
        }
        d.getItems()
                .forEach(i -> ficha.fila(
                        i.getDescripcion(),
                        i.getCantidad().stripTrailingZeros().toPlainString(),
                        Formato.moneda(i.getPrecioUnitario().setScale(2, java.math.RoundingMode.HALF_UP)),
                        Formato.moneda(i.getTotal())));
        if (d.getOpExonerada().signum() > 0 && d.getOpGravada().signum() == 0) {
            ficha.total("Op. exonerada", Formato.moneda(d.getOpExonerada())).total("IGV", Formato.moneda(d.getIgv()));
        } else if (d.getOpInafecta().signum() > 0 && d.getOpGravada().signum() == 0) {
            ficha.total("Op. inafecta", Formato.moneda(d.getOpInafecta())).total("IGV", Formato.moneda(d.getIgv()));
        } else {
            ficha.total("Op. gravada", Formato.moneda(d.getOpGravada())).total("IGV", Formato.moneda(d.getIgv()));
        }
        ficha.total("Total", Formato.moneda(d.getTotal()));
        ficha.nota("Importe en letras", NumeroALetras.soles(d.getTotal()));
        if (d.getEstado() == EstadoCpe.ANULADO) {
            ficha.banner("COMPROBANTE ANULADO");
        }
        String qr = contenidoQr(d, c);
        ficha.grafico(2, QrGenerador.generar(qr, 240));
        ficha.parametros().put("generadoPor", generadoPor);
        ficha.parametros().put("generadoEn", Formato.fechaHora(java.time.LocalDateTime.now()));
        ficha.parametros()
                .put("codigo", d.getEstado().aceptado() ? "Constancia " + d.getCdrCodigo() : "Sin constancia");
        ficha.parametros().put("urlVerificacion", propiedades.urlVerificacion());
        String hash = d.getDigestValue() == null ? "" : d.getDigestValue();
        ficha.parametros().put("hashCorto", hash);
        return motor.renderizarPdf(ficha);
    }

    private static String etiquetaEstado(EstadoCpe estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente de envío";
            case ACEPTADO -> "Aceptado";
            case ACEPTADO_OBSERVACIONES -> "Aceptado con observaciones";
            case RECHAZADO -> "Rechazado";
            case ANULADO -> "Anulado";
            case ERROR -> "Error de envío";
        };
    }

    private static void autorizar(UsuarioPrincipal actor, DocumentoElectronico d) {
        List<String> roles = actor.getRoles();
        boolean interno = roles.contains(RolNombre.ROLE_ADMIN.name())
                || roles.contains(RolNombre.ROLE_FINANZAS.name())
                || roles.contains(RolNombre.ROLE_CAJA.name())
                || roles.contains(RolNombre.ROLE_GERENCIA.name());
        boolean propio = roles.contains(RolNombre.ROLE_PACIENTE.name())
                && d.getPaciente() != null
                && d.getPaciente().getUsuario().getId().equals(actor.getId());
        if (!interno && !propio) {
            throw new ForbiddenOperationException("No tiene acceso a este comprobante");
        }
    }
}
