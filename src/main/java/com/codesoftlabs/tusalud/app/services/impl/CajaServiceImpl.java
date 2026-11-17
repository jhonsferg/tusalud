package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CierreCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.CuentaPorCobrarResponse;
import com.codesoftlabs.tusalud.app.dto.response.MetodoTotalResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteIngresosResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenCajaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SesionCajaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Comprobante;
import com.codesoftlabs.tusalud.app.entities.ComprobanteItem;
import com.codesoftlabs.tusalud.app.entities.ItemOrdenLab;
import com.codesoftlabs.tusalud.app.entities.OrdenLaboratorio;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Pago;
import com.codesoftlabs.tusalud.app.entities.SesionCaja;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaCobroRepository;
import com.codesoftlabs.tusalud.app.repositories.ComprobanteRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenLabCobroRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.PagoRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionCajaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CajaServiceImpl implements CajaService {

    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");
    private static final BigDecimal LIMITE_BOLETA_SIN_DOCUMENTO = new BigDecimal("700.00");
    private static final int LIMITE_COMPROBANTES = 200;
    private static final int LIMITE_HISTORIAL = 60;
    private static final int LIMITE_PENDIENTES = 100;
    private static final int DIAS_POR_DEFECTO = 30;
    private static final int DIAS_MAXIMOS_REPORTE = 366;

    private final SesionCajaRepository sesionRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final CitaCobroRepository citaRepository;
    private final OrdenLabCobroRepository ordenRepository;
    private final PacienteRepository pacienteRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public SesionCajaResponse abrir(UsuarioPrincipal actor, AperturaCajaRequest request) {
        if (sesionRepository.findDelCajero(actor.getId(), EstadoCaja.ABIERTA).isPresent()) {
            throw new BusinessLogicException("Ya tienes una caja abierta");
        }
        Usuario cajero = usuario(actor.getId());
        SesionCaja sesion = sesionRepository.save(SesionCaja.builder()
                .cajero(cajero)
                .apertura(LocalDateTime.now())
                .montoApertura(escala(request.montoApertura()))
                .estado(EstadoCaja.ABIERTA)
                .build());
        log.info("Caja {} abierta por {}", sesion.getId(), actor.getEmail());
        return SesionCajaResponse.from(sesion);
    }

    @Override
    public SesionCajaResponse actual(UsuarioPrincipal actor) {
        return SesionCajaResponse.from(sesionRepository
                .findDelCajero(actor.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException("Caja abierta", "cajero", actor.getId())));
    }

    @Override
    @Transactional
    public SesionCajaResponse cerrar(UsuarioPrincipal actor, CierreCajaRequest request) {
        SesionCaja sesion = cajaAbierta(actor);
        BigDecimal esperado = escala(sesion.getMontoApertura().add(efectivoDe(sesion)));
        BigDecimal contado = escala(request.montoContado());
        BigDecimal diferencia = contado.subtract(esperado);
        sesion.setCierre(LocalDateTime.now());
        sesion.setMontoContado(contado);
        sesion.setMontoEsperado(esperado);
        sesion.setDiferencia(diferencia);
        sesion.setEstado(EstadoCaja.CERRADA);
        sesion.setObservaciones(textoONulo(request.observaciones()));
        if (diferencia.signum() != 0) {
            String mensaje = "La caja de " + sesion.getCajero().getNombreCompleto() + " cerró con una diferencia de S/ "
                    + diferencia.toPlainString() + " respecto de lo esperado.";
            for (Usuario admin : usuarioRepository.findActivosPorRol(RolNombre.ROLE_ADMIN)) {
                notificacionService.crear(
                        admin,
                        TipoNotificacion.CAJA_DIFERENCIA,
                        "Diferencia en el cierre de caja",
                        mensaje,
                        "/caja/historial");
            }
        }
        log.info("Caja {} cerrada con diferencia {}", sesion.getId(), diferencia);
        return SesionCajaResponse.from(sesion);
    }

    @Override
    public List<SesionCajaResponse> historial(UsuarioPrincipal actor) {
        Long cajeroId = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name()) ? null : actor.getId();
        return sesionRepository.historial(cajeroId, PageRequest.of(0, LIMITE_HISTORIAL)).stream()
                .map(SesionCajaResponse::from)
                .toList();
    }

    @Override
    public ResumenCajaResponse resumen(UsuarioPrincipal actor) {
        SesionCaja sesion = cajaAbierta(actor);
        List<Comprobante> emitidos = comprobanteRepository.findDeCaja(sesion.getId(), EstadoComprobante.EMITIDO);
        Map<MetodoPagoCaja, BigDecimal> totales = new EnumMap<>(MetodoPagoCaja.class);
        Map<MetodoPagoCaja, Long> cantidades = new EnumMap<>(MetodoPagoCaja.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Comprobante c : emitidos) {
            totales.merge(c.getMetodoPago(), c.getTotal(), BigDecimal::add);
            cantidades.merge(c.getMetodoPago(), 1L, Long::sum);
            total = total.add(c.getTotal());
        }
        List<MetodoTotalResponse> porMetodo = new ArrayList<>();
        for (MetodoPagoCaja metodo : MetodoPagoCaja.values()) {
            porMetodo.add(new MetodoTotalResponse(
                    metodo,
                    escala(totales.getOrDefault(metodo, BigDecimal.ZERO)),
                    cantidades.getOrDefault(metodo, 0L)));
        }
        return new ResumenCajaResponse(
                sesion.getId(),
                sesion.getMontoApertura(),
                emitidos.size(),
                escala(total),
                escala(sesion.getMontoApertura().add(efectivoDe(emitidos))),
                porMetodo);
    }

    @Override
    @Transactional
    public ComprobanteResponse cobrar(UsuarioPrincipal actor, CobroRequest request) {
        SesionCaja caja = cajaAbierta(actor);
        Cita cita = null;
        OrdenLaboratorio orden = null;
        Paciente paciente = null;
        List<LineaCobro> lineas;
        switch (request.origen()) {
            case CITA -> {
                cita = citaParaCobrar(request.referenciaId());
                paciente = cita.getPaciente();
                lineas = List.of(new LineaCobro(
                        "Consulta de " + cita.getEspecialidad().getNombre() + " con "
                                + cita.getMedico().getUsuario().getNombreCompleto(),
                        1,
                        cita.getMonto()));
            }
            case LABORATORIO -> {
                orden = ordenParaCobrar(request.referenciaId());
                paciente = orden.getPaciente();
                lineas = lineasDeOrden(orden);
            }
            default -> {
                if (request.items() == null || request.items().isEmpty()) {
                    throw new DatosInvalidosException("Debe indicar al menos un ítem a cobrar");
                }
                lineas = request.items().stream().map(LineaCobro::de).toList();
                if (request.pacienteId() != null) {
                    paciente = pacienteRepository
                            .findByIdWithUsuario(request.pacienteId())
                            .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
                }
            }
        }
        BigDecimal total = escala(lineas.stream().map(LineaCobro::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (total.signum() <= 0) {
            throw new DatosInvalidosException("El total a cobrar debe ser mayor a cero");
        }
        String documento = validarDocumento(request, total);

        String serie = request.tipoComprobante() == TipoComprobante.FACTURA ? "F001" : "B001";
        int numero = comprobanteRepository.ultimoNumero(serie) + 1;
        BigDecimal subtotal = total.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        Comprobante comprobante = Comprobante.builder()
                .tipo(request.tipoComprobante())
                .serie(serie)
                .numero(numero)
                .codigo(String.format("%s-%06d", serie, numero))
                .fecha(LocalDateTime.now())
                .clienteTipoDoc(request.clienteTipoDoc())
                .clienteDoc(documento)
                .clienteNombre(request.clienteNombre().trim())
                .subtotal(subtotal)
                .igv(total.subtract(subtotal))
                .total(total)
                .metodoPago(request.metodoPago())
                .estado(EstadoComprobante.EMITIDO)
                .origen(request.origen())
                .referenciaId(request.referenciaId())
                .paciente(paciente)
                .caja(caja)
                .build();
        for (LineaCobro linea : lineas) {
            comprobante.agregarItem(ComprobanteItem.builder()
                    .descripcion(linea.descripcion())
                    .cantidad(linea.cantidad())
                    .precioUnitario(linea.precioUnitario())
                    .subtotal(linea.subtotal())
                    .build());
        }
        comprobante = comprobanteRepository.save(comprobante);

        if (cita != null) {
            registrarPagoDeCita(cita, comprobante);
        }
        if (orden != null) {
            orden.setCobrado(true);
        }
        if (paciente != null) {
            notificacionService.crear(
                    paciente.getUsuario(),
                    TipoNotificacion.COMPROBANTE_EMITIDO,
                    "Comprobante emitido",
                    "Se emitió el comprobante " + comprobante.getCodigo() + " por S/ " + total.toPlainString() + ".",
                    "/pagos");
        }
        log.info(
                "Comprobante {} emitido por {} en la caja {}", comprobante.getCodigo(), actor.getEmail(), caja.getId());
        return ComprobanteResponse.from(comprobante);
    }

    @Override
    public List<CuentaPorCobrarResponse> pendientes(String q) {
        String termino = termino(q);
        List<CuentaPorCobrarResponse> cuentas = new ArrayList<>();
        for (Cita cita : citaRepository.pendientesDeCobro(termino, PageRequest.of(0, LIMITE_PENDIENTES))) {
            cuentas.add(new CuentaPorCobrarResponse(
                    OrigenComprobante.CITA,
                    cita.getId(),
                    cita.getPaciente().getId(),
                    cita.getPaciente().getUsuario().getNombreCompleto(),
                    cita.getPaciente().getDni(),
                    "Consulta de " + cita.getEspecialidad().getNombre() + " con "
                            + cita.getMedico().getUsuario().getNombreCompleto(),
                    cita.getMonto(),
                    cita.getFechaHora()));
        }
        for (OrdenLaboratorio orden :
                ordenRepository.pendientesDeCobro(termino, PageRequest.of(0, LIMITE_PENDIENTES))) {
            cuentas.add(new CuentaPorCobrarResponse(
                    OrigenComprobante.LABORATORIO,
                    orden.getId(),
                    orden.getPaciente().getId(),
                    orden.getPaciente().getUsuario().getNombreCompleto(),
                    orden.getPaciente().getDni(),
                    "Orden de laboratorio " + orden.getCodigo(),
                    orden.getTotal(),
                    orden.getFecha()));
        }
        cuentas.sort(Comparator.comparing(CuentaPorCobrarResponse::fecha).reversed());
        return cuentas;
    }

    @Override
    public List<ComprobanteResponse> comprobantes(
            LocalDate desde, LocalDate hasta, String q, EstadoComprobante estado) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        return comprobanteRepository
                .buscar(
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay(),
                        estado,
                        termino(q),
                        PageRequest.of(0, LIMITE_COMPROBANTES))
                .stream()
                .map(ComprobanteResponse::from)
                .toList();
    }

    @Override
    public ComprobanteResponse obtener(Long id) {
        return ComprobanteResponse.from(comprobante(id));
    }

    @Override
    @Transactional
    public ComprobanteResponse anular(UsuarioPrincipal actor, Long id, String motivo) {
        Comprobante comprobante = comprobante(id);
        if (comprobante.getEstado() != EstadoComprobante.EMITIDO) {
            throw new BusinessLogicException("El comprobante " + comprobante.getCodigo() + " ya está anulado");
        }
        boolean admin = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        SesionCaja caja = comprobante.getCaja();
        if (!admin) {
            if (!caja.estaAbierta()) {
                throw new BusinessLogicException("La caja de este comprobante ya fue cerrada");
            }
            if (!caja.getCajero().getId().equals(actor.getId())) {
                throw new ForbiddenOperationException("Solo puedes anular comprobantes de tu propia caja");
            }
        }
        comprobante.setEstado(EstadoComprobante.ANULADO);
        comprobante.setMotivoAnulacion(motivo.trim());
        comprobante.setAnuladoEn(LocalDateTime.now());
        revertirOrigen(comprobante);
        log.info("Comprobante {} anulado por {}", comprobante.getCodigo(), actor.getEmail());
        return ComprobanteResponse.from(comprobante);
    }

    @Override
    public ReporteIngresosResponse reporteIngresos(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        if (inicio.plusDays(DIAS_MAXIMOS_REPORTE).isBefore(fin)) {
            throw new DatosInvalidosException(
                    "El rango del reporte no puede superar " + DIAS_MAXIMOS_REPORTE + " días");
        }
        List<Comprobante> emitidos = comprobanteRepository.findEntreFechas(
                inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(), EstadoComprobante.EMITIDO);
        BigDecimal total = BigDecimal.ZERO;
        Map<LocalDate, BigDecimal> dias = new TreeMap<>();
        Map<LocalDate, Long> cantidades = new TreeMap<>();
        Map<MetodoPagoCaja, BigDecimal> metodos = new EnumMap<>(MetodoPagoCaja.class);
        Map<OrigenComprobante, BigDecimal> origenes = new EnumMap<>(OrigenComprobante.class);
        for (Comprobante c : emitidos) {
            LocalDate dia = c.getFecha().toLocalDate();
            total = total.add(c.getTotal());
            dias.merge(dia, c.getTotal(), BigDecimal::add);
            cantidades.merge(dia, 1L, Long::sum);
            metodos.merge(c.getMetodoPago(), c.getTotal(), BigDecimal::add);
            origenes.merge(c.getOrigen(), c.getTotal(), BigDecimal::add);
        }
        return new ReporteIngresosResponse(
                escala(total),
                dias.entrySet().stream()
                        .map(e -> new ReporteIngresosResponse.Dia(
                                e.getKey(), escala(e.getValue()), cantidades.get(e.getKey())))
                        .toList(),
                metodos.entrySet().stream()
                        .map(e -> new ReporteIngresosResponse.Metodo(e.getKey(), escala(e.getValue())))
                        .toList(),
                origenes.entrySet().stream()
                        .map(e -> new ReporteIngresosResponse.Origen(e.getKey(), escala(e.getValue())))
                        .toList());
    }

    private void revertirOrigen(Comprobante comprobante) {
        Long referencia = comprobante.getReferenciaId();
        if (referencia == null) {
            return;
        }
        if (comprobante.getOrigen() == OrigenComprobante.CITA) {
            citaRepository.findParaCobro(referencia).ifPresent(cita -> {
                Pago pago = cita.getPago();
                if (pago != null && pago.getEstado() == EstadoPago.PAGADO) {
                    pago.setEstado(EstadoPago.REEMBOLSADO);
                    notificacionService.crear(
                            cita.getPaciente().getUsuario(),
                            TipoNotificacion.PAGO_REEMBOLSADO,
                            "Pago anulado",
                            "Se anuló el comprobante " + comprobante.getCodigo() + " de su cita.",
                            "/citas/" + cita.getId());
                }
            });
        } else if (comprobante.getOrigen() == OrigenComprobante.LABORATORIO) {
            ordenRepository.findParaCobro(referencia).ifPresent(orden -> orden.setCobrado(false));
        }
    }

    private void registrarPagoDeCita(Cita cita, Comprobante comprobante) {
        Pago pago = cita.getPago();
        String descripcion = etiqueta(comprobante.getMetodoPago()) + " en caja";
        String codigo = "CJ-" + comprobante.getCodigo();
        if (pago == null) {
            pago = Pago.builder().cita(cita).build();
        }
        pago.setMetodoPago(null);
        pago.setMetodoDescripcion(descripcion);
        pago.setMonto(comprobante.getTotal());
        pago.setDescuento(escala(BigDecimal.ZERO));
        pago.setTotal(comprobante.getTotal());
        pago.setCodigoCupon(null);
        pago.setEstado(EstadoPago.PAGADO);
        pago.setComprobante(codigo);
        pago.setFechaPago(comprobante.getFecha());
        pago = pagoRepository.save(pago);
        cita.setPago(pago);
    }

    private Cita citaParaCobrar(Long citaId) {
        if (citaId == null) {
            throw new DatosInvalidosException("Debe indicar la cita a cobrar");
        }
        Cita cita = citaRepository
                .findParaCobro(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessLogicException("No se puede cobrar una cita cancelada");
        }
        if (cita.getPago() != null && cita.getPago().getEstado() == EstadoPago.PAGADO) {
            throw new BusinessLogicException("La cita ya tiene un pago registrado");
        }
        if (comprobanteRepository.existsByOrigenAndReferenciaIdAndEstado(
                OrigenComprobante.CITA, citaId, EstadoComprobante.EMITIDO)) {
            throw new BusinessLogicException("La cita ya tiene un comprobante emitido");
        }
        return cita;
    }

    private OrdenLaboratorio ordenParaCobrar(Long ordenId) {
        if (ordenId == null) {
            throw new DatosInvalidosException("Debe indicar la orden de laboratorio a cobrar");
        }
        OrdenLaboratorio orden = ordenRepository
                .findParaCobro(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de laboratorio", "id", ordenId));
        if (orden.getEstado() == EstadoOrdenLab.CANCELADA) {
            throw new BusinessLogicException("No se puede cobrar una orden cancelada");
        }
        if (orden.isCobrado()
                || comprobanteRepository.existsByOrigenAndReferenciaIdAndEstado(
                        OrigenComprobante.LABORATORIO, ordenId, EstadoComprobante.EMITIDO)) {
            throw new BusinessLogicException("La orden ya tiene un comprobante emitido");
        }
        return orden;
    }

    private List<LineaCobro> lineasDeOrden(OrdenLaboratorio orden) {
        List<LineaCobro> lineas = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (ItemOrdenLab item : orden.getItems()) {
            LineaCobro linea = new LineaCobro(
                    item.getExamen().getNombre(), 1, item.getExamen().getPrecio());
            lineas.add(linea);
            suma = suma.add(linea.subtotal());
        }
        if (lineas.isEmpty() || suma.compareTo(escala(orden.getTotal())) != 0) {
            return List.of(new LineaCobro("Orden de laboratorio " + orden.getCodigo(), 1, orden.getTotal()));
        }
        return lineas;
    }

    private String validarDocumento(CobroRequest request, BigDecimal total) {
        String doc = textoONulo(request.clienteDoc());
        TipoDocumentoCliente tipoDoc = request.clienteTipoDoc();
        if (request.tipoComprobante() == TipoComprobante.FACTURA) {
            if (tipoDoc != TipoDocumentoCliente.RUC || doc == null || !doc.matches("\\d{11}")) {
                throw new DatosInvalidosException("La factura exige un RUC de 11 dígitos");
            }
            return doc;
        }
        if (tipoDoc == TipoDocumentoCliente.RUC) {
            throw new DatosInvalidosException("La boleta no admite RUC, emita una factura");
        }
        if (tipoDoc == TipoDocumentoCliente.DNI) {
            if (doc == null || !doc.matches("\\d{8}")) {
                throw new DatosInvalidosException("El DNI debe tener 8 dígitos");
            }
            return doc;
        }
        if (total.compareTo(LIMITE_BOLETA_SIN_DOCUMENTO) > 0) {
            throw new DatosInvalidosException("Las boletas mayores a S/ 700.00 exigen el DNI del cliente");
        }
        return null;
    }

    private SesionCaja cajaAbierta(UsuarioPrincipal actor) {
        return sesionRepository
                .findDelCajero(actor.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new BusinessLogicException("No tienes una caja abierta"));
    }

    private BigDecimal efectivoDe(SesionCaja sesion) {
        return efectivoDe(comprobanteRepository.findDeCaja(sesion.getId(), EstadoComprobante.EMITIDO));
    }

    private BigDecimal efectivoDe(List<Comprobante> comprobantes) {
        return comprobantes.stream()
                .filter(c -> c.getMetodoPago() == MetodoPagoCaja.EFECTIVO)
                .map(Comprobante::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Comprobante comprobante(Long id) {
        return comprobanteRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", "id", id));
    }

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String termino(String q) {
        return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static String etiqueta(MetodoPagoCaja metodo) {
        return switch (metodo) {
            case EFECTIVO -> "Efectivo";
            case TARJETA -> "Tarjeta";
            case YAPE -> "Yape";
            case PLIN -> "Plin";
            case TRANSFERENCIA -> "Transferencia";
        };
    }

    private record LineaCobro(String descripcion, int cantidad, BigDecimal precioUnitario) {

        static LineaCobro de(CobroItemRequest item) {
            return new LineaCobro(item.descripcion().trim(), item.cantidad(), item.precioUnitario());
        }

        BigDecimal subtotal() {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
