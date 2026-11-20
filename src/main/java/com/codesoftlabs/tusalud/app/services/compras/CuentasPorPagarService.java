package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.ComprobanteProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.PagoProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComprobanteProveedorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ResumenCuentasPorPagar;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.SaldoProveedor;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.TramoVencimiento;
import com.codesoftlabs.tusalud.app.entities.ComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.OrdenCompra;
import com.codesoftlabs.tusalud.app.entities.OrdenCompraItem;
import com.codesoftlabs.tusalud.app.entities.PagoProveedor;
import com.codesoftlabs.tusalud.app.entities.Proveedor;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.facturacion.ValidadorDocumentos;
import com.codesoftlabs.tusalud.app.repositories.ComprobanteProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenCompraRepository;
import com.codesoftlabs.tusalud.app.repositories.PagoProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.ProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuentasPorPagarService {

    private static final int LIMITE = 300;
    private static final int DIAS_POR_DEFECTO = 180;
    private static final String ENTIDAD = "COMPROBANTE";
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");
    private static final String ENLACE = "/finanzas/compras/cuentas-por-pagar";

    private final ComprobanteProveedorRepository comprobanteRepository;
    private final PagoProveedorRepository pagoRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final ComprasAuditor auditor;
    private final ComprasAvisos avisos;
    private final org.springframework.context.ApplicationEventPublisher eventos;

    public List<ComprobanteProveedorResponse> listar(
            EstadoComprobanteProveedor estado,
            UUID proveedorId,
            LocalDate desde,
            LocalDate hasta,
            boolean soloVencidos) {
        LocalDate hoy = LocalDate.now();
        LocalDate fin = hasta == null ? hoy.plusYears(1) : hasta;
        boolean abiertos = soloVencidos
                || estado == EstadoComprobanteProveedor.PENDIENTE
                || estado == EstadoComprobanteProveedor.PARCIAL;
        LocalDate inicio =
                desde != null ? desde : abiertos ? LocalDate.of(2000, 1, 1) : hoy.minusDays(DIAS_POR_DEFECTO);
        return comprobanteRepository
                .buscar(estado, proveedorId, inicio, fin, soloVencidos, hoy, PageRequest.of(0, LIMITE))
                .stream()
                .map(c -> ComprasMapper.comprobante(c, List.of(), hoy))
                .toList();
    }

    public ComprobanteProveedorResponse obtener(UUID id) {
        return respuesta(comprobante(id));
    }

    @Transactional
    public ComprobanteProveedorResponse registrar(UsuarioPrincipal actor, ComprobanteProveedorRequest request) {
        if (!ValidadorDocumentos.rucValido(request.rucEmisor())) {
            throw new DatosInvalidosException("El RUC del emisor no es válido");
        }
        Proveedor proveedor = proveedorRepository
                .findById(request.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", request.proveedorId()));
        if (!proveedor.getRuc().equals(request.rucEmisor())) {
            throw new ConflictException("El RUC del comprobante no coincide con el del proveedor");
        }
        String serie = request.serie().strip().toUpperCase();
        String numero = request.numero().strip();
        if (comprobanteRepository.existsByProveedorIdAndTipoAndSerieAndNumero(
                proveedor.getId(), request.tipo(), serie, numero)) {
            throw new ConflictException(
                    "Ya se registró el comprobante " + serie + "-" + numero + " de " + proveedor.getRazonSocial());
        }
        LocalDate hoy = LocalDate.now();
        if (request.fechaEmision().isAfter(hoy)) {
            throw new ConflictException("La fecha de emisión no puede ser futura");
        }
        BigDecimal subtotal = request.subtotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = request.igv() == null ? BigDecimal.ZERO : request.igv().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv);
        OrdenCompra orden = null;
        int plazo = proveedor.getCondicionPagoDias();
        if (request.ordenId() != null) {
            orden = ordenRepository
                    .findConDetalle(request.ordenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Orden de compra", "id", request.ordenId()));
            validarContraOrden(orden, proveedor, total);
            plazo = orden.getCondicionPagoDias();
        }
        LocalDate vencimiento = request.fechaVencimiento() == null
                ? request.fechaEmision().plusDays(plazo)
                : request.fechaVencimiento();
        if (vencimiento.isBefore(request.fechaEmision())) {
            throw new ConflictException("El vencimiento no puede ser anterior a la emisión");
        }
        Usuario usuario = usuario(actor);
        ComprobanteProveedor comprobante = comprobanteRepository.save(ComprobanteProveedor.builder()
                .proveedor(proveedor)
                .orden(orden)
                .tipo(request.tipo())
                .serie(serie)
                .numero(numero)
                .rucEmisor(request.rucEmisor())
                .fechaEmision(request.fechaEmision())
                .fechaVencimiento(vencimiento)
                .subtotal(subtotal)
                .igv(igv)
                .total(total)
                .estado(EstadoComprobanteProveedor.PENDIENTE)
                .observaciones(limpiar(request.observaciones()))
                .registradoPor(usuario)
                .registradoEn(LocalDateTime.now())
                .build());
        auditor.registrar(
                usuario,
                ENTIDAD,
                comprobante.getId(),
                "REGISTRAR",
                comprobante.numeroCompleto() + " " + proveedor.getRazonSocial() + " S/ " + total);
        eventos.publishEvent(new com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraRegistrada(
                comprobante.getId(),
                comprobante.numeroCompleto(),
                proveedor.getRazonSocial(),
                comprobante.getFechaEmision(),
                subtotal,
                igv,
                total));
        return respuesta(comprobante);
    }

    @Transactional
    public ComprobanteProveedorResponse pagar(UsuarioPrincipal actor, UUID id, PagoProveedorRequest request) {
        comprobanteRepository
                .findParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", "id", id));
        ComprobanteProveedor comprobante = comprobante(id);
        if (comprobante.getEstado() != EstadoComprobanteProveedor.PENDIENTE
                && comprobante.getEstado() != EstadoComprobanteProveedor.PARCIAL) {
            throw new ConflictException("El comprobante " + comprobante.numeroCompleto() + " no admite pagos");
        }
        BigDecimal monto = request.monto().setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(comprobante.saldo()) > 0) {
            throw new ConflictException("El pago excede el saldo de S/ " + comprobante.saldo());
        }
        LocalDate fecha = request.fecha() == null ? LocalDate.now() : request.fecha();
        if (fecha.isAfter(LocalDate.now())) {
            throw new ConflictException("La fecha del pago no puede ser futura");
        }
        Usuario usuario = usuario(actor);
        PagoProveedor pago = pagoRepository.save(PagoProveedor.builder()
                .comprobante(comprobante)
                .monto(monto)
                .metodo(request.metodo())
                .referencia(limpiar(request.referencia()))
                .fecha(fecha)
                .registradoPor(usuario)
                .registradoEn(LocalDateTime.now())
                .build());
        comprobante.setMontoPagado(comprobante.getMontoPagado().add(monto));
        comprobante.setEstado(
                comprobante.saldo().signum() == 0
                        ? EstadoComprobanteProveedor.PAGADO
                        : EstadoComprobanteProveedor.PARCIAL);
        auditor.registrar(
                usuario,
                ENTIDAD,
                id,
                "PAGAR",
                "S/ " + monto + " " + request.metodo() + " saldo " + comprobante.saldo());
        eventos.publishEvent(new com.codesoftlabs.tusalud.app.services.events.HechosContables.PagoProveedorRegistrado(
                pago.getId(),
                id,
                comprobante.numeroCompleto(),
                comprobante.getProveedor().getRazonSocial(),
                fecha,
                monto,
                request.metodo()));
        return respuesta(comprobante);
    }

    @Transactional
    public ComprobanteProveedorResponse anular(UsuarioPrincipal actor, UUID id, String motivo) {
        comprobanteRepository
                .findParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", "id", id));
        ComprobanteProveedor comprobante = comprobante(id);
        if (comprobante.getEstado() != EstadoComprobanteProveedor.PENDIENTE
                || comprobante.getMontoPagado().signum() > 0
                || comprobante.getMontoCredito().signum() > 0) {
            throw new ConflictException("Solo se anula un comprobante sin pagos ni créditos");
        }
        comprobante.setEstado(EstadoComprobanteProveedor.ANULADO);
        comprobante.setAnuladoEn(LocalDateTime.now());
        comprobante.setMotivoAnulacion(motivo.strip());
        auditor.registrar(usuario(actor), ENTIDAD, id, "ANULAR", motivo);
        eventos.publishEvent(new com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraAnulada(
                id, comprobante.numeroCompleto(), LocalDate.now(), motivo.strip()));
        return respuesta(comprobante);
    }

    public ResumenCuentasPorPagar resumen() {
        LocalDate hoy = LocalDate.now();
        List<ComprobanteProveedor> abiertos = comprobanteRepository.deEstados(
                EnumSet.of(EstadoComprobanteProveedor.PENDIENTE, EstadoComprobanteProveedor.PARCIAL));
        BigDecimal[] montos = new BigDecimal[5];
        long[] cantidades = new long[5];
        for (int i = 0; i < montos.length; i++) {
            montos[i] = BigDecimal.ZERO;
        }
        BigDecimal pendiente = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        long vencidas = 0;
        Map<UUID, BigDecimal[]> porProveedor = new LinkedHashMap<>();
        Map<UUID, String> nombres = new LinkedHashMap<>();
        Map<UUID, long[]> conteos = new LinkedHashMap<>();
        for (ComprobanteProveedor c : abiertos) {
            BigDecimal saldo = c.saldo();
            pendiente = pendiente.add(saldo);
            long dias = ChronoUnit.DAYS.between(c.getFechaVencimiento(), hoy);
            int tramo = dias <= 0 ? 0 : dias <= 30 ? 1 : dias <= 60 ? 2 : dias <= 90 ? 3 : 4;
            montos[tramo] = montos[tramo].add(saldo);
            cantidades[tramo]++;
            boolean estaVencido = dias > 0;
            if (estaVencido) {
                vencido = vencido.add(saldo);
                vencidas++;
            }
            UUID proveedorId = c.getProveedor().getId();
            BigDecimal[] acumulado =
                    porProveedor.computeIfAbsent(proveedorId, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            acumulado[0] = acumulado[0].add(saldo);
            if (estaVencido) {
                acumulado[1] = acumulado[1].add(saldo);
            }
            nombres.put(proveedorId, c.getProveedor().getRazonSocial());
            conteos.computeIfAbsent(proveedorId, k -> new long[1])[0]++;
        }
        String[] etiquetas = {"Por vencer", "1 a 30 días", "31 a 60 días", "61 a 90 días", "Más de 90 días"};
        List<TramoVencimiento> tramos = new ArrayList<>();
        for (int i = 0; i < etiquetas.length; i++) {
            tramos.add(new TramoVencimiento(etiquetas[i], montos[i], cantidades[i]));
        }
        List<SaldoProveedor> proveedores = porProveedor.entrySet().stream()
                .map(e -> new SaldoProveedor(
                        e.getKey(),
                        nombres.get(e.getKey()),
                        e.getValue()[0],
                        e.getValue()[1],
                        conteos.get(e.getKey())[0]))
                .sorted((a, b) -> b.saldo().compareTo(a.saldo()))
                .toList();
        return new ResumenCuentasPorPagar(pendiente, vencido, abiertos.size(), vencidas, tramos, proveedores);
    }

    @Scheduled(cron = "${app.compras.cron-vencimientos:0 0 8 * * *}")
    @Transactional
    public void ejecutarProgramado() {
        int enviadas = notificarVencidos(LocalDateTime.now());
        log.debug("Avisos de cuentas por pagar vencidas: {}", enviadas);
    }

    @Transactional
    public int notificarVencidos(LocalDateTime ahora) {
        ResumenCuentasPorPagar resumen = resumen();
        if (resumen.cantidadVencidas() == 0) {
            return 0;
        }
        LocalDateTime inicioDia = ahora.toLocalDate().atStartOfDay();
        int enviadas = 0;
        for (Usuario destinatario : usuarioRepository.findActivosPorRol(RolNombre.ROLE_FINANZAS)) {
            if (notificacionRepository.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                    destinatario.getId(), TipoNotificacion.CUENTA_POR_PAGAR_VENCIDA, inicioDia)) {
                continue;
            }
            avisos.avisarUsuario(
                    destinatario,
                    TipoNotificacion.CUENTA_POR_PAGAR_VENCIDA,
                    "Cuentas por pagar vencidas",
                    resumen.cantidadVencidas() + " comprobantes vencidos suman S/ " + resumen.totalVencido() + ".",
                    ENLACE);
            enviadas++;
        }
        return enviadas;
    }

    private void validarContraOrden(OrdenCompra orden, Proveedor proveedor, BigDecimal total) {
        if (!orden.getProveedor().getId().equals(proveedor.getId())) {
            throw new ConflictException("La orden es de otro proveedor");
        }
        if (!orden.getEstado().aprobada()) {
            throw new ConflictException("La orden " + orden.codigoVisible() + " no está aprobada");
        }
        BigDecimal recibido = BigDecimal.ZERO;
        for (OrdenCompraItem item : orden.getItems()) {
            recibido = recibido.add(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidadRecibida())));
        }
        if (orden.isAfectaIgv()) {
            recibido = recibido.add(recibido.multiply(OrdenCompra.TASA_IGV));
        }
        if (recibido.signum() == 0) {
            throw new ConflictException("La orden " + orden.codigoVisible() + " aún no tiene mercadería recibida");
        }
        BigDecimal facturado = comprobanteRepository.totalFacturadoDeOrden(orden.getId());
        BigDecimal limite = recibido.add(recibido.multiply(TOLERANCIA)).setScale(2, RoundingMode.HALF_UP);
        if (facturado.add(total).compareTo(limite) > 0) {
            throw new ConflictException("El comprobante excede lo recibido de la orden, que es S/ "
                    + recibido.setScale(2, RoundingMode.HALF_UP) + " con S/ " + facturado + " ya facturados");
        }
    }

    private ComprobanteProveedor comprobante(UUID id) {
        return comprobanteRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", "id", id));
    }

    private ComprobanteProveedorResponse respuesta(ComprobanteProveedor comprobante) {
        return ComprasMapper.comprobante(
                comprobante, pagoRepository.deComprobante(comprobante.getId()), LocalDate.now());
    }

    private Usuario usuario(UsuarioPrincipal actor) {
        return usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
