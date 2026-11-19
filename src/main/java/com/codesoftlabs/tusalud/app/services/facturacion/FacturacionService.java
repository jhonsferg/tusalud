package com.codesoftlabs.tusalud.app.services.facturacion;

import com.codesoftlabs.tusalud.app.config.FacturacionProperties;
import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.CorreccionClienteRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.NotaItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.NotaRequest;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.DocumentoElectronicoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.DocumentoElectronicoResumen;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.EstadoFacturacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.IntercambioResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.ItemCpeResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.MotivoNotaResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.ResumenSunatResponse;
import com.codesoftlabs.tusalud.app.entities.Comprobante;
import com.codesoftlabs.tusalud.app.entities.ComprobanteItem;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronico;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronicoItem;
import com.codesoftlabs.tusalud.app.entities.IntercambioSunat;
import com.codesoftlabs.tusalud.app.entities.ResumenSunat;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.Afectacion;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoResumen;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.OperacionSunat;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoResumen;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.facturacion.AlmacenFiscal;
import com.codesoftlabs.tusalud.app.facturacion.CertificadoDigital;
import com.codesoftlabs.tusalud.app.facturacion.PaqueteSunat;
import com.codesoftlabs.tusalud.app.facturacion.ProveedorFacturacion;
import com.codesoftlabs.tusalud.app.facturacion.ProveedorFacturacion.RespuestaEnvio;
import com.codesoftlabs.tusalud.app.facturacion.ProveedorFacturacion.RespuestaTicket;
import com.codesoftlabs.tusalud.app.facturacion.UblGenerador;
import com.codesoftlabs.tusalud.app.facturacion.ValidadorDocumentos;
import com.codesoftlabs.tusalud.app.facturacion.XmlFirmador;
import com.codesoftlabs.tusalud.app.repositories.DocumentoElectronicoRepository;
import com.codesoftlabs.tusalud.app.repositories.IntercambioSunatRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.ResumenSunatRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacturacionService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int LIMITE_LISTA = 300;
    private static final List<MotivoNotaResponse> MOTIVOS = List.of(
            new MotivoNotaResponse("01", "Anulación de la operación", true, TipoCpe.NOTA_CREDITO),
            new MotivoNotaResponse("02", "Anulación por error en el RUC", true, TipoCpe.NOTA_CREDITO),
            new MotivoNotaResponse("06", "Devolución total", true, TipoCpe.NOTA_CREDITO),
            new MotivoNotaResponse("07", "Devolución por ítem", false, TipoCpe.NOTA_CREDITO),
            new MotivoNotaResponse("09", "Disminución en el valor", false, TipoCpe.NOTA_CREDITO),
            new MotivoNotaResponse("13", "Ajustes de montos y fechas de pago", false, TipoCpe.NOTA_CREDITO),
            new MotivoNotaResponse("01", "Intereses por mora", false, TipoCpe.NOTA_DEBITO),
            new MotivoNotaResponse("02", "Aumento en el valor", false, TipoCpe.NOTA_DEBITO),
            new MotivoNotaResponse("03", "Penalidades u otros conceptos", false, TipoCpe.NOTA_DEBITO));

    private final ConfiguracionFiscalService configuracion;
    private final DocumentoElectronicoRepository documentos;
    private final ResumenSunatRepository resumenes;
    private final IntercambioSunatRepository intercambios;
    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final UblGenerador generador;
    private final XmlFirmador firmador;
    private final CertificadoDigital certificado;
    private final AlmacenFiscal almacen;
    private final List<ProveedorFacturacion> proveedores;
    private final NotificacionService notificaciones;
    private final FacturacionProperties propiedades;

    public List<MotivoNotaResponse> motivos() {
        return MOTIVOS;
    }

    @Transactional
    public Optional<DocumentoElectronico> registrarDesdeComprobante(Comprobante comprobante) {
        Optional<ConfiguracionFiscal> vigente = configuracion.vigente();
        if (vigente.isEmpty()
                || documentos.findDeComprobante(comprobante.getId()).isPresent()) {
            return Optional.empty();
        }
        ConfiguracionFiscal c = vigente.get();
        TipoCpe tipo = comprobante.getTipo() == TipoComprobante.FACTURA ? TipoCpe.FACTURA : TipoCpe.BOLETA;
        boolean porResumen = tipo == TipoCpe.BOLETA && c.isBoletasPorResumen();
        DocumentoElectronico d = DocumentoElectronico.builder()
                .tipo(tipo)
                .serie(comprobante.getSerie())
                .numero(comprobante.getNumero())
                .sede(c.getSede())
                .emitidoEn(comprobante.getFecha())
                .comprobante(comprobante)
                .clienteTipoDoc(comprobante.getClienteTipoDoc())
                .clienteDoc(comprobante.getClienteDoc())
                .clienteNombre(comprobante.getClienteNombre())
                .total(comprobante.getTotal())
                .estado(EstadoCpe.PENDIENTE)
                .proximoIntento(porResumen ? null : LocalDateTime.now())
                .paciente(comprobante.getPaciente())
                .creadoPor(comprobante.getCaja().getCajero())
                .creadoEn(LocalDateTime.now())
                .build();
        String unidad = comprobante.getOrigen() == OrigenComprobante.FARMACIA ? "NIU" : "ZZ";
        BigDecimal divisor = divisor(c);
        for (ComprobanteItem item : comprobante.getItems()) {
            d.agregarItem(itemDesdeTotal(
                    item.getDescripcion(),
                    unidad,
                    BigDecimal.valueOf(item.getCantidad()),
                    item.getPrecioUnitario(),
                    item.getSubtotal(),
                    c.getAfectacionServicios(),
                    divisor));
        }
        totalizar(d, comprobante.getSubtotal(), comprobante.getIgv());
        documentos.save(d);
        configuracion.sincronizar(tipo, d.getSerie(), d.getNumero());
        generarYAlmacenar(d, c);
        return Optional.of(d);
    }

    @Transactional
    public DocumentoElectronicoDetalle crearNota(UsuarioPrincipal actor, NotaRequest r) {
        if (!r.tipo().esNota()) {
            throw new DatosInvalidosException("El tipo debe ser nota de crédito o nota de débito");
        }
        ConfiguracionFiscal c = configuracion.exigir();
        if (!c.isHabilitada()) {
            throw new ConflictException("La facturación electrónica está deshabilitada");
        }
        DocumentoElectronico ref = documentos
                .findConDetalle(r.documentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante electrónico", "id", r.documentoId()));
        if (ref.getTipo().esNota()) {
            throw new ConflictException("Solo se pueden emitir notas sobre facturas y boletas");
        }
        if (!ref.getEstado().aceptado()) {
            throw new ConflictException("El comprobante debe estar aceptado por SUNAT para emitir una nota");
        }
        MotivoNotaResponse motivo = MOTIVOS.stream()
                .filter(m -> m.tipo() == r.tipo() && m.codigo().equals(r.motivoCodigo()))
                .findFirst()
                .orElseThrow(() -> new DatosInvalidosException("El motivo no corresponde a una nota de este tipo"));
        String serie = configuracion
                .serieDeNota(r.tipo(), ref.getTipo())
                .orElseThrow(() -> new DatosInvalidosException(
                        "No hay una serie activa de " + r.tipo().etiqueta().toLowerCase(Locale.ROOT) + " para "
                                + ref.getTipo().etiqueta().toLowerCase(Locale.ROOT)));
        DocumentoElectronico nota = DocumentoElectronico.builder()
                .tipo(r.tipo())
                .serie(serie)
                .sede(c.getSede())
                .emitidoEn(LocalDateTime.now())
                .documentoReferencia(ref)
                .motivoCodigo(motivo.codigo())
                .motivoDescripcion(r.descripcion().trim())
                .clienteTipoDoc(ref.getClienteTipoDoc())
                .clienteDoc(ref.getClienteDoc())
                .clienteNombre(ref.getClienteNombre())
                .clienteDireccion(ref.getClienteDireccion())
                .estado(EstadoCpe.PENDIENTE)
                .proximoIntento(LocalDateTime.now())
                .paciente(ref.getPaciente())
                .creadoPor(usuarios.getReferenceById(actor.getId()))
                .creadoEn(LocalDateTime.now())
                .total(BigDecimal.ZERO)
                .build();
        BigDecimal divisor = divisor(c);
        if (motivo.total()) {
            if (r.tipo() == TipoCpe.NOTA_CREDITO) {
                for (DocumentoElectronicoItem origen : ref.getItems()) {
                    nota.agregarItem(DocumentoElectronicoItem.builder()
                            .codigo(origen.getCodigo())
                            .descripcion(origen.getDescripcion())
                            .unidad(origen.getUnidad())
                            .cantidad(origen.getCantidad())
                            .valorUnitario(origen.getValorUnitario())
                            .precioUnitario(origen.getPrecioUnitario())
                            .afectacion(origen.getAfectacion())
                            .valorVenta(origen.getValorVenta())
                            .igv(origen.getIgv())
                            .total(origen.getTotal())
                            .build());
                }
                totalizar(nota, ref.getOpGravada(), ref.getIgv());
            }
        } else {
            if (r.items() == null || r.items().isEmpty()) {
                throw new DatosInvalidosException("Indique los ítems de la nota");
            }
            Afectacion porDefecto = ref.getItems().isEmpty()
                    ? c.getAfectacionServicios()
                    : ref.getItems().get(0).getAfectacion();
            for (NotaItemRequest item : r.items()) {
                Afectacion afectacion = item.afectacion() == null ? porDefecto : item.afectacion();
                BigDecimal total =
                        item.cantidad().multiply(item.precioUnitario()).setScale(2, RoundingMode.HALF_UP);
                nota.agregarItem(itemDesdeTotal(
                        item.descripcion().trim(),
                        "ZZ",
                        item.cantidad(),
                        item.precioUnitario(),
                        total,
                        afectacion,
                        divisor));
            }
            totalizar(nota, null, null);
        }
        if (nota.getItems().isEmpty() || nota.getTotal().signum() <= 0) {
            throw new DatosInvalidosException("La nota debe tener un importe mayor a cero");
        }
        if (r.tipo() == TipoCpe.NOTA_CREDITO) {
            BigDecimal acreditado = documentos.sumaNotas(
                    ref.getId(),
                    TipoCpe.NOTA_CREDITO,
                    EnumSet.of(EstadoCpe.PENDIENTE, EstadoCpe.ACEPTADO, EstadoCpe.ACEPTADO_OBSERVACIONES));
            if (acreditado.add(nota.getTotal()).compareTo(ref.getTotal()) > 0) {
                throw new ConflictException("Las notas de crédito superan el importe del comprobante original");
            }
        }
        nota.setNumero(configuracion.siguienteNumero(serie));
        documentos.save(nota);
        generarYAlmacenar(nota, c);
        log.info("Nota {} emitida por {} sobre {}", nota.numeroCompleto(), actor.getId(), ref.numeroCompleto());
        enviar(nota.getId());
        return detalle(nota.getId());
    }

    public List<DocumentoElectronicoResumen> listar(
            LocalDate desde, LocalDate hasta, TipoCpe tipo, EstadoCpe estado, String q) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(30) : desde;
        if (inicio.isAfter(fin)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        String termino = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return documentos
                .buscar(
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay(),
                        tipo,
                        estado,
                        termino,
                        PageRequest.of(0, LIMITE_LISTA))
                .stream()
                .map(DocumentoElectronicoResumen::de)
                .toList();
    }

    public DocumentoElectronicoDetalle detalle(UUID id) {
        return detalle(cargar(id));
    }

    public Optional<DocumentoElectronicoDetalle> deComprobante(UUID comprobanteId) {
        return documentos
                .findDeComprobante(comprobanteId)
                .flatMap(d -> documentos.findConDetalle(d.getId()))
                .map(this::detalle);
    }

    public byte[] xml(UUID id) {
        DocumentoElectronico d = cargar(id);
        if (d.getXmlClave() == null) {
            throw new ResourceNotFoundException("XML del comprobante", "id", id);
        }
        return almacen.leer(d.getXmlClave());
    }

    public byte[] cdr(UUID id) {
        DocumentoElectronico d = cargar(id);
        if (d.getCdrClave() == null) {
            throw new ResourceNotFoundException("Constancia de recepción", "id", id);
        }
        return almacen.leer(d.getCdrClave());
    }

    @Transactional
    public void enviar(UUID id) {
        DocumentoElectronico d = cargar(id);
        if (d.getEstado() != EstadoCpe.PENDIENTE || d.getResumen() != null) {
            return;
        }
        ConfiguracionFiscal c = configuracion.exigir();
        if (d.getXmlClave() == null) {
            generarYAlmacenar(d, c);
        }
        byte[] xml = almacen.leer(d.getXmlClave());
        String nombre = nombreArchivo(c, d);
        byte[] zip = PaqueteSunat.comprimir(nombre + ".xml", xml);
        ProveedorFacturacion proveedor = proveedor(c);
        long inicio = System.nanoTime();
        RespuestaEnvio respuesta;
        try {
            respuesta = proveedor.enviarComprobante(nombre + ".zip", zip, c);
        } catch (RuntimeException ex) {
            respuesta = RespuestaEnvio.transitorio("Error al enviar el comprobante: " + ex.getMessage());
        }
        int duracion = (int) Duration.ofNanos(System.nanoTime() - inicio).toMillis();
        registrarIntercambio(d.getId(), null, OperacionSunat.ENVIO_COMPROBANTE, c, respuesta, duracion, zip);
        d.setIntentos(d.getIntentos() + 1);
        if (respuesta.aceptado()) {
            aceptar(d, respuesta);
        } else if (respuesta.transitorio()) {
            reprogramar(d, respuesta.descripcion());
        } else {
            rechazar(d, respuesta);
        }
    }

    @Transactional
    public DocumentoElectronicoDetalle reenviar(UsuarioPrincipal actor, UUID id) {
        DocumentoElectronico d = cargar(id);
        if (d.getEstado() != EstadoCpe.ERROR && d.getEstado() != EstadoCpe.RECHAZADO) {
            throw new ConflictException("Solo se pueden reenviar los comprobantes con error o rechazados");
        }
        d.setEstado(EstadoCpe.PENDIENTE);
        d.setIntentos(0);
        d.setUltimoError(null);
        d.setProximoIntento(LocalDateTime.now());
        log.info("Reenvío de {} solicitado por {}", d.numeroCompleto(), actor.getId());
        enviar(id);
        return detalle(id);
    }

    @Transactional
    public DocumentoElectronicoDetalle corregirCliente(UsuarioPrincipal actor, UUID id, CorreccionClienteRequest r) {
        DocumentoElectronico d = cargar(id);
        if (d.getEstado() != EstadoCpe.PENDIENTE
                && d.getEstado() != EstadoCpe.ERROR
                && d.getEstado() != EstadoCpe.RECHAZADO) {
            throw new ConflictException("Un comprobante aceptado no se puede modificar, emita una nota de crédito");
        }
        String documento = r.documento() == null || r.documento().isBlank()
                ? null
                : r.documento().trim();
        if (d.getTipo() == TipoCpe.FACTURA || r.tipoDoc() == TipoDocumentoCliente.RUC) {
            if (r.tipoDoc() != TipoDocumentoCliente.RUC || !ValidadorDocumentos.rucValido(documento)) {
                throw new DatosInvalidosException("El RUC del cliente no es válido");
            }
        } else if (r.tipoDoc() == TipoDocumentoCliente.DNI && !ValidadorDocumentos.dniValido(documento)) {
            throw new DatosInvalidosException("El DNI del cliente no es válido");
        } else if (r.tipoDoc() == TipoDocumentoCliente.SIN_DOC) {
            documento = null;
        }
        d.setClienteTipoDoc(r.tipoDoc());
        d.setClienteDoc(documento);
        d.setClienteNombre(r.nombre().trim());
        d.setClienteDireccion(
                r.direccion() == null || r.direccion().isBlank()
                        ? null
                        : r.direccion().trim());
        Comprobante comprobante = d.getComprobante();
        if (comprobante != null) {
            comprobante.setClienteTipoDoc(d.getClienteTipoDoc());
            comprobante.setClienteDoc(d.getClienteDoc());
            comprobante.setClienteNombre(d.getClienteNombre());
        }
        d.setEstado(EstadoCpe.PENDIENTE);
        d.setIntentos(0);
        d.setUltimoError(null);
        d.setCdrCodigo(null);
        d.setCdrDescripcion(null);
        d.setProximoIntento(LocalDateTime.now());
        generarYAlmacenar(d, configuracion.exigir());
        log.info("Datos del cliente de {} corregidos por {}", d.numeroCompleto(), actor.getId());
        enviar(id);
        return detalle(id);
    }

    @Transactional
    public DocumentoElectronicoDetalle solicitarBaja(UsuarioPrincipal actor, UUID id, String motivo) {
        DocumentoElectronico d = cargar(id);
        solicitarBaja(d, actor == null ? null : actor.getId(), motivo);
        return detalle(id);
    }

    @Transactional
    public void anularPorComprobante(UsuarioPrincipal actor, Comprobante comprobante, String motivo) {
        Optional<DocumentoElectronico> encontrado = documentos.findDeComprobante(comprobante.getId());
        if (encontrado.isEmpty()) {
            return;
        }
        DocumentoElectronico d =
                documentos.findConDetalle(encontrado.get().getId()).orElseThrow();
        if (d.getEstado() == EstadoCpe.ANULADO) {
            return;
        }
        if (d.getEstado().aceptado()) {
            solicitarBaja(d, actor.getId(), motivo);
            return;
        }
        d.setEstado(EstadoCpe.ANULADO);
        d.setProximoIntento(null);
        d.setMotivoBaja(motivo);
        d.setUltimoError(null);
    }

    @Transactional
    public ResumenSunatResponse generarResumenDiario(UsuarioPrincipal actor, LocalDate fecha) {
        ConfiguracionFiscal c = configuracion.exigir();
        if (fecha.isAfter(LocalDate.now())) {
            throw new DatosInvalidosException("No se puede resumir una fecha futura");
        }
        List<DocumentoElectronico> boletas = documentos.paraResumen(
                TipoCpe.BOLETA,
                EstadoCpe.PENDIENTE,
                fecha.atStartOfDay(),
                fecha.plusDays(1).atStartOfDay());
        if (boletas.isEmpty()) {
            throw new ConflictException("No hay boletas pendientes de resumir en esa fecha");
        }
        ResumenSunat resumen =
                nuevoResumen(TipoResumen.RESUMEN_DIARIO, fecha, null, actor == null ? null : actor.getId());
        boletas.forEach(b -> {
            b.setResumen(resumen);
            b.setProximoIntento(null);
        });
        almacenarResumen(resumen, boletas, c);
        return ResumenSunatResponse.de(resumen, boletas.size());
    }

    public List<ResumenSunatResponse> listarResumenes() {
        return resumenes.recientes(PageRequest.of(0, 60)).stream()
                .map(r -> ResumenSunatResponse.de(
                        r, documentos.deResumen(r.getId()).size()))
                .toList();
    }

    @Transactional
    public void procesarResumen(UUID id) {
        ResumenSunat r = resumenes.findById(id).orElseThrow();
        ConfiguracionFiscal c = configuracion.exigir();
        ProveedorFacturacion proveedor = proveedor(c);
        long inicio = System.nanoTime();
        try {
            if (r.getEstado() == EstadoResumen.PENDIENTE) {
                String nombre = c.getRuc() + "-" + r.getIdentificador();
                byte[] zip = PaqueteSunat.comprimir(nombre + ".xml", almacen.leer(r.getXmlClave()));
                r.setIntentos(r.getIntentos() + 1);
                String ticket = proveedor.enviarResumen(nombre + ".zip", zip, c);
                r.setTicket(ticket);
                r.setEstado(EstadoResumen.ENVIADO);
                r.setProximoIntento(LocalDateTime.now().plusSeconds(2));
                intercambios.save(intercambio(
                        null,
                        r.getId(),
                        OperacionSunat.ENVIO_RESUMEN,
                        c,
                        true,
                        "0",
                        "Ticket " + ticket,
                        inicio,
                        CodigoDocumentoClinico.sha256(zip)));
            } else if (r.getEstado() == EstadoResumen.ENVIADO) {
                RespuestaTicket respuesta = proveedor.consultarTicket(r.getTicket(), c);
                intercambios.save(intercambio(
                        null,
                        r.getId(),
                        OperacionSunat.CONSULTA_TICKET,
                        c,
                        !RespuestaTicket.RECHAZADO.equals(respuesta.estado()),
                        respuesta.codigo(),
                        respuesta.descripcion(),
                        inicio,
                        null));
                resolverResumen(r, respuesta);
            }
            r.setUltimoError(null);
        } catch (RuntimeException ex) {
            r.setUltimoError(recortar(ex.getMessage(), 400));
            if (r.getIntentos() >= propiedades.cola().intentosMaximos()) {
                r.setEstado(EstadoResumen.ERROR);
                r.setProximoIntento(null);
            } else {
                r.setProximoIntento(LocalDateTime.now().plus(retroceso(Math.max(1, r.getIntentos()))));
            }
        }
    }

    public List<UUID> pendientesDeEnvio(int limite) {
        return documentos.listosParaEnviar(EstadoCpe.PENDIENTE, LocalDateTime.now(), PageRequest.of(0, limite));
    }

    public List<UUID> resumenesPendientes(int limite) {
        return resumenes.listos(
                EnumSet.of(EstadoResumen.PENDIENTE, EstadoResumen.ENVIADO),
                LocalDateTime.now(),
                PageRequest.of(0, limite));
    }

    public EstadoFacturacionResponse estado() {
        Optional<ConfiguracionFiscal> c = configuracion.actual();
        Map<EstadoCpe, Long> conteo = new EnumMap<>(EstadoCpe.class);
        for (EstadoCpe e : EstadoCpe.values()) {
            conteo.put(e, 0L);
        }
        for (Object[] fila :
                documentos.conteoPorEstado(LocalDate.now().minusDays(30).atStartOfDay())) {
            conteo.put((EstadoCpe) fila[0], (Long) fila[1]);
        }
        long pendientes = pendientesDeEnvio(1000).size();
        boolean prueba = propiedades.certificado().ruta() == null
                || propiedades.certificado().ruta().isBlank();
        return new EstadoFacturacionResponse(
                c.map(ConfiguracionFiscal::isHabilitada).orElse(false),
                c.map(ConfiguracionFiscal::getProveedor).orElse(null),
                c.map(ConfiguracionFiscal::getModo).orElse(null),
                prueba,
                conteo,
                pendientes,
                null);
    }

    public List<DocumentoElectronicoResumen> misComprobantes(UsuarioPrincipal actor) {
        return pacientes
                .findByUsuarioId(actor.getId())
                .map(p -> documentos.delPaciente(p.getId(), PageRequest.of(0, LIMITE_LISTA)).stream()
                        .map(DocumentoElectronicoResumen::de)
                        .toList())
                .orElse(List.of());
    }

    public DocumentoElectronico cargar(UUID id) {
        return documentos
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante electrónico", "id", id));
    }

    public Optional<ConfiguracionFiscal> configuracionVigente() {
        return configuracion.actual();
    }

    private void solicitarBaja(DocumentoElectronico d, UUID actorId, String motivo) {
        if (!d.getEstado().aceptado()) {
            throw new ConflictException("Solo se puede dar de baja un comprobante aceptado por SUNAT");
        }
        if (d.getBajaResumen() != null) {
            throw new ConflictException("El comprobante ya tiene una baja en trámite");
        }
        ConfiguracionFiscal c = configuracion.exigir();
        long dias = Duration.between(d.getEmitidoEn(), LocalDateTime.now()).toDays();
        if (dias > c.getDiasLimiteBaja()) {
            throw new ConflictException(
                    "Pasó el plazo de " + c.getDiasLimiteBaja() + " días para la baja. Emita una nota de crédito");
        }
        BigDecimal acreditado = documentos.sumaNotas(
                d.getId(),
                TipoCpe.NOTA_CREDITO,
                EnumSet.of(EstadoCpe.PENDIENTE, EstadoCpe.ACEPTADO, EstadoCpe.ACEPTADO_OBSERVACIONES));
        if (acreditado.signum() > 0) {
            throw new ConflictException("El comprobante tiene notas de crédito, no corresponde dar de baja");
        }
        d.setMotivoBaja(motivo.trim());
        TipoResumen tipo = d.getTipo() == TipoCpe.BOLETA ? TipoResumen.RESUMEN_DIARIO : TipoResumen.COMUNICACION_BAJA;
        ResumenSunat resumen = nuevoResumen(tipo, d.getEmitidoEn().toLocalDate(), motivo.trim(), actorId);
        d.setBajaResumen(resumen);
        almacenarResumen(resumen, List.of(d), c);
        log.info("Baja de {} en trámite mediante {}", d.numeroCompleto(), resumen.getIdentificador());
    }

    private void resolverResumen(ResumenSunat r, RespuestaTicket respuesta) {
        List<DocumentoElectronico> afectados = documentos.deResumen(r.getId());
        if (RespuestaTicket.PENDIENTE.equals(respuesta.estado())) {
            r.setProximoIntento(LocalDateTime.now()
                    .plusSeconds(retroceso(Math.max(1, r.getIntentos())).toSeconds()));
            return;
        }
        r.setCdrCodigo(respuesta.codigo());
        r.setCdrDescripcion(recortar(respuesta.descripcion(), 400));
        r.setProximoIntento(null);
        if (respuesta.cdrZip() != null) {
            r.setCdrClave(almacen.guardarZip("cdr-resumen", r.getId(), respuesta.cdrZip()));
        }
        if (RespuestaTicket.ACEPTADO.equals(respuesta.estado())) {
            r.setEstado(EstadoResumen.ACEPTADO);
            for (DocumentoElectronico d : afectados) {
                if (d.getBajaResumen() != null && d.getBajaResumen().getId().equals(r.getId())) {
                    d.setEstado(EstadoCpe.ANULADO);
                } else if (d.getResumen() != null && d.getResumen().getId().equals(r.getId())) {
                    d.setEstado(EstadoCpe.ACEPTADO);
                    d.setCdrCodigo(respuesta.codigo());
                    d.setCdrDescripcion(recortar(respuesta.descripcion(), 400));
                    d.setCdrRecibidoEn(LocalDateTime.now());
                }
            }
        } else {
            r.setEstado(EstadoResumen.RECHAZADO);
            for (DocumentoElectronico d : afectados) {
                if (d.getBajaResumen() != null && d.getBajaResumen().getId().equals(r.getId())) {
                    d.setBajaResumen(null);
                    d.setMotivoBaja(null);
                } else if (d.getResumen() != null && d.getResumen().getId().equals(r.getId())) {
                    d.setResumen(null);
                    d.setProximoIntento(LocalDateTime.now());
                }
            }
            notificarRechazoResumen(r);
        }
    }

    private ResumenSunat nuevoResumen(TipoResumen tipo, LocalDate referencia, String motivo, UUID actorId) {
        LocalDate hoy = LocalDate.now();
        long correlativo = resumenes.contarDelDia(tipo, hoy) + 1;
        String identificador = tipo.prefijo() + "-" + hoy.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + correlativo;
        return resumenes.save(ResumenSunat.builder()
                .tipo(tipo)
                .identificador(identificador)
                .fechaReferencia(referencia)
                .fechaGeneracion(hoy)
                .estado(EstadoResumen.PENDIENTE)
                .motivo(motivo)
                .proximoIntento(LocalDateTime.now())
                .creadoPor(actorId == null ? null : usuarios.getReferenceById(actorId))
                .creadoEn(LocalDateTime.now())
                .build());
    }

    private void almacenarResumen(ResumenSunat resumen, List<DocumentoElectronico> docs, ConfiguracionFiscal c) {
        byte[] xml = resumen.getTipo() == TipoResumen.RESUMEN_DIARIO
                ? generador.resumenDiario(resumen, docs, c)
                : generador.comunicacionBaja(resumen, docs, c);
        XmlFirmador.XmlFirmado firmado = firmador.firmar(xml, certificado.credencial());
        resumen.setXmlSha256(CodigoDocumentoClinico.sha256(firmado.contenido()));
        resumen.setXmlClave(almacen.guardarXml("resumen", resumen.getId(), firmado.contenido()));
    }

    private void generarYAlmacenar(DocumentoElectronico d, ConfiguracionFiscal c) {
        try {
            byte[] xml = generador.comprobante(d, c);
            XmlFirmador.XmlFirmado firmado = firmador.firmar(xml, certificado.credencial());
            d.setDigestValue(firmado.digest());
            d.setXmlSha256(CodigoDocumentoClinico.sha256(firmado.contenido()));
            d.setXmlClave(almacen.guardarXml("comprobante", d.getId(), firmado.contenido()));
        } catch (RuntimeException ex) {
            log.error("No se pudo generar el XML de {}", d.numeroCompleto(), ex);
            d.setEstado(EstadoCpe.ERROR);
            d.setProximoIntento(null);
            d.setUltimoError(recortar("No se pudo generar el XML: " + ex.getMessage(), 400));
        }
    }

    private void aceptar(DocumentoElectronico d, RespuestaEnvio respuesta) {
        boolean observado =
                respuesta.observaciones() != null && !respuesta.observaciones().isEmpty();
        d.setEstado(observado ? EstadoCpe.ACEPTADO_OBSERVACIONES : EstadoCpe.ACEPTADO);
        d.setCdrCodigo(respuesta.codigo());
        String descripcion = respuesta.descripcion();
        if (observado) {
            descripcion = descripcion + ". " + String.join("; ", respuesta.observaciones());
        }
        d.setCdrDescripcion(recortar(descripcion, 400));
        d.setCdrRecibidoEn(LocalDateTime.now());
        d.setProximoIntento(null);
        d.setUltimoError(null);
        if (respuesta.cdrZip() != null) {
            d.setCdrClave(almacen.guardarZip("cdr", d.getId(), respuesta.cdrZip()));
        }
    }

    private void rechazar(DocumentoElectronico d, RespuestaEnvio respuesta) {
        d.setEstado(EstadoCpe.RECHAZADO);
        d.setCdrCodigo(respuesta.codigo());
        d.setCdrDescripcion(recortar(respuesta.descripcion(), 400));
        d.setProximoIntento(null);
        notificar(
                "Comprobante rechazado",
                "SUNAT rechazó el comprobante " + d.numeroCompleto() + ": " + recortar(respuesta.descripcion(), 120));
    }

    private void reprogramar(DocumentoElectronico d, String motivo) {
        d.setUltimoError(recortar(motivo, 400));
        if (d.getIntentos() >= propiedades.cola().intentosMaximos()) {
            d.setEstado(EstadoCpe.ERROR);
            d.setProximoIntento(null);
            notificar(
                    "Comprobante sin enviar",
                    "No se pudo enviar el comprobante " + d.numeroCompleto() + " tras " + d.getIntentos()
                            + " intentos. Reenvíelo manualmente.");
        } else {
            d.setProximoIntento(LocalDateTime.now().plus(retroceso(d.getIntentos())));
        }
    }

    private Duration retroceso(int intentos) {
        long segundos = (long) propiedades.cola().retrocesoBaseSegundos() * (1L << Math.min(intentos - 1, 10));
        return Duration.ofSeconds(Math.min(segundos, 3600));
    }

    private void notificarRechazoResumen(ResumenSunat r) {
        notificar(
                "Resumen rechazado",
                "SUNAT rechazó " + r.getIdentificador() + ": " + recortar(r.getCdrDescripcion(), 120));
    }

    private void notificar(String titulo, String mensaje) {
        List<Usuario> destinatarios = new ArrayList<>(usuarios.findActivosPorRol(RolNombre.ROLE_FINANZAS));
        usuarios.findActivosPorRol(RolNombre.ROLE_ADMIN).stream()
                .filter(u -> destinatarios.stream().noneMatch(x -> x.getId().equals(u.getId())))
                .forEach(destinatarios::add);
        for (Usuario u : destinatarios) {
            notificaciones.crear(
                    u, TipoNotificacion.COMPROBANTE_RECHAZADO, titulo, mensaje, "/comprobantes-electronicos");
        }
    }

    private void registrarIntercambio(
            UUID documentoId,
            UUID resumenId,
            OperacionSunat operacion,
            ConfiguracionFiscal c,
            RespuestaEnvio respuesta,
            int duracion,
            byte[] zip) {
        intercambios.save(IntercambioSunat.builder()
                .documentoId(documentoId)
                .resumenId(resumenId)
                .operacion(operacion)
                .proveedor(c.getProveedor())
                .exitoso(respuesta.aceptado())
                .codigoRespuesta(respuesta.codigo())
                .mensaje(recortar(respuesta.descripcion(), 500))
                .duracionMs(duracion)
                .hashEnvio(CodigoDocumentoClinico.sha256(zip))
                .creadoEn(LocalDateTime.now())
                .build());
    }

    private IntercambioSunat intercambio(
            UUID documentoId,
            UUID resumenId,
            OperacionSunat operacion,
            ConfiguracionFiscal c,
            boolean exitoso,
            String codigo,
            String mensaje,
            long inicioNanos,
            String hash) {
        return IntercambioSunat.builder()
                .documentoId(documentoId)
                .resumenId(resumenId)
                .operacion(operacion)
                .proveedor(c.getProveedor())
                .exitoso(exitoso)
                .codigoRespuesta(codigo)
                .mensaje(recortar(mensaje, 500))
                .duracionMs(
                        (int) Duration.ofNanos(System.nanoTime() - inicioNanos).toMillis())
                .hashEnvio(hash)
                .creadoEn(LocalDateTime.now())
                .build();
    }

    private ProveedorFacturacion proveedor(ConfiguracionFiscal c) {
        return proveedores.stream()
                .filter(p -> p.atiende(c.getProveedor()))
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException("No hay un proveedor para " + c.getProveedor()));
    }

    private DocumentoElectronicoDetalle detalle(DocumentoElectronico d) {
        List<ItemCpeResponse> items =
                d.getItems().stream().map(ItemCpeResponse::de).toList();
        List<IntercambioResponse> traza = intercambios.findByDocumentoIdOrderByCreadoEnAsc(d.getId()).stream()
                .map(IntercambioResponse::de)
                .toList();
        BigDecimal acreditado = documentos.sumaNotas(
                d.getId(),
                TipoCpe.NOTA_CREDITO,
                EnumSet.of(EstadoCpe.PENDIENTE, EstadoCpe.ACEPTADO, EstadoCpe.ACEPTADO_OBSERVACIONES));
        DocumentoElectronico ref = d.getDocumentoReferencia();
        return new DocumentoElectronicoDetalle(
                d.getId(),
                d.getTipo(),
                d.getSerie(),
                d.getNumero(),
                d.numeroCompleto(),
                d.getEmitidoEn(),
                d.getMoneda(),
                d.getClienteTipoDoc(),
                d.getClienteDoc(),
                d.getClienteNombre(),
                d.getClienteDireccion(),
                d.getOpGravada(),
                d.getOpExonerada(),
                d.getOpInafecta(),
                d.getIgv(),
                d.getTotal(),
                d.getEstado(),
                d.getMotivoCodigo(),
                d.getMotivoDescripcion(),
                d.getDigestValue(),
                d.getXmlSha256(),
                d.getCdrCodigo(),
                d.getCdrDescripcion(),
                d.getCdrRecibidoEn(),
                d.getIntentos(),
                d.getProximoIntento(),
                d.getUltimoError(),
                d.getMotivoBaja(),
                d.getComprobante() == null ? null : d.getComprobante().getId(),
                ref == null ? null : ref.getId(),
                ref == null ? null : ref.numeroCompleto(),
                acreditado,
                items,
                traza);
    }

    private static String nombreArchivo(ConfiguracionFiscal c, DocumentoElectronico d) {
        return c.getRuc() + "-" + d.getTipo().codigo() + "-" + d.getSerie() + "-" + d.getNumero();
    }

    private static BigDecimal divisor(ConfiguracionFiscal c) {
        return BigDecimal.ONE.add(c.getIgvPorcentaje().divide(CIEN, 4, RoundingMode.HALF_UP));
    }

    private static DocumentoElectronicoItem itemDesdeTotal(
            String descripcion,
            String unidad,
            BigDecimal cantidad,
            BigDecimal precio,
            BigDecimal total,
            Afectacion afectacion,
            BigDecimal divisor) {
        BigDecimal totalLinea = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorUnitario;
        BigDecimal valorVenta;
        BigDecimal igv;
        if (afectacion == Afectacion.GRAVADO) {
            valorUnitario = precio.divide(divisor, 6, RoundingMode.HALF_UP);
            valorVenta = totalLinea.divide(divisor, 2, RoundingMode.HALF_UP);
            igv = totalLinea.subtract(valorVenta);
        } else {
            valorUnitario = precio.setScale(6, RoundingMode.HALF_UP);
            valorVenta = totalLinea;
            igv = BigDecimal.ZERO.setScale(2);
        }
        return DocumentoElectronicoItem.builder()
                .descripcion(recortar(descripcion, 200))
                .unidad(unidad)
                .cantidad(cantidad.setScale(3, RoundingMode.HALF_UP))
                .valorUnitario(valorUnitario)
                .precioUnitario(precio.setScale(6, RoundingMode.HALF_UP))
                .afectacion(afectacion)
                .valorVenta(valorVenta)
                .igv(igv)
                .total(totalLinea)
                .build();
    }

    private static void totalizar(DocumentoElectronico d, BigDecimal baseGravada, BigDecimal igvTotal) {
        BigDecimal gravada = BigDecimal.ZERO;
        BigDecimal exonerada = BigDecimal.ZERO;
        BigDecimal inafecta = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (DocumentoElectronicoItem item : d.getItems()) {
            switch (item.getAfectacion()) {
                case GRAVADO -> gravada = gravada.add(item.getValorVenta());
                case EXONERADO -> exonerada = exonerada.add(item.getValorVenta());
                case INAFECTO -> inafecta = inafecta.add(item.getValorVenta());
            }
            igv = igv.add(item.getIgv());
            total = total.add(item.getTotal());
        }
        if (baseGravada != null
                && igvTotal != null
                && !d.getItems().isEmpty()
                && exonerada.signum() == 0
                && inafecta.signum() == 0) {
            DocumentoElectronicoItem ultimo = d.getItems().get(d.getItems().size() - 1);
            ultimo.setValorVenta(ultimo.getValorVenta().add(baseGravada.subtract(gravada)));
            ultimo.setIgv(ultimo.getIgv().add(igvTotal.subtract(igv)));
            gravada = baseGravada;
            igv = igvTotal;
        }
        d.setOpGravada(gravada.setScale(2, RoundingMode.HALF_UP));
        d.setOpExonerada(exonerada.setScale(2, RoundingMode.HALF_UP));
        d.setOpInafecta(inafecta.setScale(2, RoundingMode.HALF_UP));
        d.setIgv(igv.setScale(2, RoundingMode.HALF_UP));
        d.setTotal(total.setScale(2, RoundingMode.HALF_UP));
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
