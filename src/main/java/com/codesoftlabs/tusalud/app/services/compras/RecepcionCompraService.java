package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.DevolucionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.DevolucionRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RecepcionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RecepcionRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.DevolucionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionResumen;
import com.codesoftlabs.tusalud.app.dto.response.IngresoResponse;
import com.codesoftlabs.tusalud.app.dto.response.LoteResponse;
import com.codesoftlabs.tusalud.app.entities.ComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.DevolucionItem;
import com.codesoftlabs.tusalud.app.entities.DevolucionProveedor;
import com.codesoftlabs.tusalud.app.entities.Lote;
import com.codesoftlabs.tusalud.app.entities.MovimientoInventario;
import com.codesoftlabs.tusalud.app.entities.OrdenCompra;
import com.codesoftlabs.tusalud.app.entities.OrdenCompraItem;
import com.codesoftlabs.tusalud.app.entities.RecepcionCompra;
import com.codesoftlabs.tusalud.app.entities.RecepcionItem;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ComprobanteProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.DevolucionProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.IngresoRepository;
import com.codesoftlabs.tusalud.app.repositories.LoteRepository;
import com.codesoftlabs.tusalud.app.repositories.MovimientoInventarioRepository;
import com.codesoftlabs.tusalud.app.repositories.RecepcionCompraRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecepcionCompraService {

    private static final int LIMITE = 200;
    private static final int DIAS_POR_DEFECTO = 90;
    private static final String ENTIDAD_RECEPCION = "RECEPCION";
    private static final String ENTIDAD_DEVOLUCION = "DEVOLUCION";

    private final OrdenCompraService ordenes;
    private final RecepcionCompraRepository recepcionRepository;
    private final DevolucionProveedorRepository devolucionRepository;
    private final ComprobanteProveedorRepository comprobanteRepository;
    private final LoteRepository loteRepository;
    private final IngresoRepository ingresoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlmacenService almacenService;
    private final ComprasSecuencias secuencias;
    private final ComprasAuditor auditor;

    public List<RecepcionResumen> listar(UUID ordenId, boolean soloDiscrepancias, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        return recepcionRepository
                .buscar(
                        ordenId,
                        soloDiscrepancias,
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay(),
                        PageRequest.of(0, LIMITE))
                .stream()
                .map(ComprasMapper::recepcionResumen)
                .toList();
    }

    public RecepcionDetalle obtener(UUID id) {
        return ComprasMapper.recepcionDetalle(recepcionRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recepción", "id", id)));
    }

    @Transactional
    public RecepcionDetalle registrar(UsuarioPrincipal actor, UUID ordenId, RecepcionRequest request) {
        OrdenCompra orden = ordenes.ordenParaActualizar(ordenId);
        if (!orden.getEstado().admiteRecepcion()) {
            throw new ConflictException("La orden " + orden.codigoVisible() + " no admite recepciones");
        }
        verificarSegregacion(actor, orden);
        Usuario usuario = usuario(actor);
        Map<UUID, OrdenCompraItem> itemsOrden = new HashMap<>();
        orden.getItems().forEach(i -> itemsOrden.put(i.getId(), i));
        Set<UUID> vistos = new HashSet<>();
        List<IngresoItemRequest> paraIngreso = new ArrayList<>();
        boolean discrepancia = false;
        boolean algo = false;
        LocalDate hoy = LocalDate.now();
        for (RecepcionItemRequest item : request.items()) {
            OrdenCompraItem ordenItem = itemsOrden.get(item.ordenItemId());
            if (ordenItem == null) {
                throw new ConflictException("El ítem no pertenece a la orden");
            }
            if (!vistos.add(item.ordenItemId())) {
                throw new ConflictException(
                        "El ítem de " + ordenItem.getProducto().getNombre() + " está repetido");
            }
            int recibida = item.cantidadRecibida();
            int rechazada = item.cantidadRechazada() == null ? 0 : item.cantidadRechazada();
            if (recibida + rechazada > ordenItem.pendiente()) {
                throw new ConflictException("Se recibe más de lo pendiente de "
                        + ordenItem.getProducto().getNombre() + ", pendiente " + ordenItem.pendiente());
            }
            if (recibida + rechazada == 0) {
                continue;
            }
            algo = true;
            if (rechazada > 0 && limpiar(item.motivoDiscrepancia()) == null) {
                throw new ConflictException("Indique el motivo del rechazo de "
                        + ordenItem.getProducto().getNombre());
            }
            if (rechazada > 0 || limpiar(item.motivoDiscrepancia()) != null) {
                discrepancia = true;
            }
            if (recibida > 0) {
                if (limpiar(item.numeroLote()) == null || item.fechaVencimiento() == null) {
                    throw new ConflictException("Indique el lote y el vencimiento de "
                            + ordenItem.getProducto().getNombre());
                }
                if (!item.fechaVencimiento().isAfter(hoy)) {
                    throw new ConflictException(
                            "El lote de " + ordenItem.getProducto().getNombre() + " llega vencido");
                }
                paraIngreso.add(new IngresoItemRequest(
                        ordenItem.getProducto().getId(),
                        item.numeroLote().strip(),
                        item.fechaVencimiento(),
                        recibida,
                        ordenItem.getPrecioUnitario(),
                        limpiar(item.ubicacion())));
            }
        }
        if (!algo) {
            throw new ConflictException("La recepción no tiene cantidades");
        }
        IngresoResponse ingreso = null;
        if (!paraIngreso.isEmpty()) {
            ingreso = almacenService.registrarIngreso(
                    actor,
                    new IngresoRequest(
                            orden.getProveedor().getId(),
                            limpiar(request.guiaRemision()),
                            "Recepción de " + orden.codigoVisible(),
                            paraIngreso));
        }
        Map<String, Lote> lotes = new HashMap<>();
        if (ingreso != null) {
            for (LoteResponse lote : ingreso.items()) {
                lotes.put(
                        lote.productoId() + "|" + lote.numeroLote().toUpperCase(),
                        loteRepository.findById(lote.id()).orElseThrow());
            }
        }
        int anio = hoy.getYear();
        RecepcionCompra recepcion = RecepcionCompra.builder()
                .codigo(ComprasSecuencias.formatear(
                        ComprasSecuencias.RECEPCION, anio, secuencias.siguiente(ComprasSecuencias.RECEPCION, anio)))
                .orden(orden)
                .guiaRemision(limpiar(request.guiaRemision()))
                .observaciones(limpiar(request.observaciones()))
                .recibidaPor(usuario)
                .recibidaEn(LocalDateTime.now())
                .ingreso(ingreso == null ? null : ingresoRepository.getReferenceById(ingreso.id()))
                .conDiscrepancia(discrepancia)
                .build();
        for (RecepcionItemRequest item : request.items()) {
            OrdenCompraItem ordenItem = itemsOrden.get(item.ordenItemId());
            int recibida = item.cantidadRecibida();
            int rechazada = item.cantidadRechazada() == null ? 0 : item.cantidadRechazada();
            if (recibida + rechazada == 0) {
                continue;
            }
            Lote lote = recibida == 0
                    ? null
                    : lotes.get(ordenItem.getProducto().getId() + "|"
                            + item.numeroLote().strip().toUpperCase());
            recepcion.agregarItem(RecepcionItem.builder()
                    .ordenItem(ordenItem)
                    .producto(ordenItem.getProducto())
                    .cantidadRecibida(recibida)
                    .cantidadRechazada(rechazada)
                    .numeroLote(recibida == 0 ? null : item.numeroLote().strip())
                    .fechaVencimiento(recibida == 0 ? null : item.fechaVencimiento())
                    .ubicacion(limpiar(item.ubicacion()))
                    .lote(lote)
                    .costoUnitario(ordenItem.getPrecioUnitario())
                    .motivoDiscrepancia(limpiar(item.motivoDiscrepancia()))
                    .build());
            ordenItem.setCantidadRecibida(ordenItem.getCantidadRecibida() + recibida);
            ordenItem.setCantidadRechazada(ordenItem.getCantidadRechazada() + rechazada);
        }
        recepcionRepository.save(recepcion);
        boolean completa = orden.getItems().stream().allMatch(i -> i.pendiente() == 0);
        orden.setEstado(completa ? EstadoOrdenCompra.RECIBIDA : EstadoOrdenCompra.PARCIAL);
        auditor.registrar(
                usuario,
                ENTIDAD_RECEPCION,
                recepcion.getId(),
                "REGISTRAR",
                recepcion.getCodigo() + " de " + orden.codigoVisible() + (discrepancia ? " con discrepancias" : ""));
        return ComprasMapper.recepcionDetalle(recepcion);
    }

    public List<DevolucionResponse> devoluciones(UUID proveedorId, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        return devolucionRepository
                .buscar(proveedorId, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(), PageRequest.of(0, LIMITE))
                .stream()
                .map(ComprasMapper::devolucion)
                .toList();
    }

    @Transactional
    public DevolucionResponse devolver(UsuarioPrincipal actor, DevolucionRequest request) {
        RecepcionCompra recepcion = recepcionRepository
                .findConDetalle(request.recepcionId())
                .orElseThrow(() -> new ResourceNotFoundException("Recepción", "id", request.recepcionId()));
        Usuario usuario = usuario(actor);
        ComprobanteProveedor comprobante = null;
        if (request.comprobanteId() != null) {
            comprobante = comprobanteRepository
                    .findParaActualizar(request.comprobanteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comprobante", "id", request.comprobanteId()));
            comprobante = comprobanteRepository
                    .findConDetalle(request.comprobanteId())
                    .orElseThrow();
            if (!comprobante
                    .getProveedor()
                    .getId()
                    .equals(recepcion.getOrden().getProveedor().getId())) {
                throw new ConflictException("El comprobante es de otro proveedor");
            }
            if (comprobante.getEstado() == EstadoComprobanteProveedor.ANULADO) {
                throw new ConflictException("El comprobante está anulado");
            }
        }
        Map<UUID, RecepcionItem> itemsRecepcion = new HashMap<>();
        recepcion.getItems().forEach(i -> itemsRecepcion.put(i.getId(), i));
        String codigo = ComprasSecuencias.formatear(
                ComprasSecuencias.DEVOLUCION,
                LocalDate.now().getYear(),
                secuencias.siguiente(
                        ComprasSecuencias.DEVOLUCION, LocalDate.now().getYear()));
        DevolucionProveedor devolucion = DevolucionProveedor.builder()
                .codigo(codigo)
                .proveedor(recepcion.getOrden().getProveedor())
                .recepcion(recepcion)
                .comprobante(comprobante)
                .motivo(request.motivo().strip())
                .monto(BigDecimal.ZERO)
                .registradaPor(usuario)
                .registradaEn(LocalDateTime.now())
                .build();
        Set<UUID> vistos = new HashSet<>();
        BigDecimal base = BigDecimal.ZERO;
        for (DevolucionItemRequest item : request.items()) {
            RecepcionItem recibido = itemsRecepcion.get(item.recepcionItemId());
            if (recibido == null || recibido.getLote() == null) {
                throw new ConflictException("El ítem no pertenece a la recepción o no ingresó al inventario");
            }
            if (!vistos.add(item.recepcionItemId())) {
                throw new ConflictException("El ítem está repetido en la devolución");
            }
            int disponible = recibido.getCantidadRecibida() - recibido.getCantidadDevuelta();
            if (item.cantidad() > disponible) {
                throw new ConflictException("Solo se pueden devolver " + disponible + " de "
                        + recibido.getProducto().getNombre());
            }
            Lote lote = loteRepository
                    .findByIdParaActualizar(recibido.getLote().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lote", "id", recibido.getLote().getId()));
            if (item.cantidad() > lote.getCantidadActual()) {
                throw new ConflictException("El lote " + lote.getNumeroLote() + " ya no tiene " + item.cantidad()
                        + " unidades en el almacén");
            }
            lote.setCantidadActual(lote.getCantidadActual() - item.cantidad());
            recibido.setCantidadDevuelta(recibido.getCantidadDevuelta() + item.cantidad());
            movimientoRepository.save(MovimientoInventario.builder()
                    .fecha(LocalDateTime.now())
                    .tipo(MotivoMovimiento.DEVOLUCION_PROVEEDOR.tipo())
                    .motivo(MotivoMovimiento.DEVOLUCION_PROVEEDOR)
                    .producto(lote.getProducto())
                    .lote(lote)
                    .cantidad(item.cantidad())
                    .usuario(usuario)
                    .referencia(codigo)
                    .observaciones(request.motivo().strip())
                    .build());
            devolucion.agregarItem(DevolucionItem.builder()
                    .recepcionItem(recibido)
                    .lote(lote)
                    .cantidad(item.cantidad())
                    .costoUnitario(recibido.getCostoUnitario())
                    .build());
            base = base.add(recibido.getCostoUnitario().multiply(BigDecimal.valueOf(item.cantidad())));
        }
        BigDecimal monto = recepcion.getOrden().isAfectaIgv()
                ? base.add(base.multiply(OrdenCompra.TASA_IGV)).setScale(2, RoundingMode.HALF_UP)
                : base.setScale(2, RoundingMode.HALF_UP);
        devolucion.setMonto(monto);
        if (comprobante != null) {
            if (monto.compareTo(comprobante.saldo()) > 0) {
                throw new ConflictException("La devolución excede el saldo del comprobante");
            }
            comprobante.setMontoCredito(comprobante.getMontoCredito().add(monto));
            if (comprobante.saldo().signum() == 0) {
                comprobante.setEstado(EstadoComprobanteProveedor.PAGADO);
            }
        }
        devolucionRepository.save(devolucion);
        auditor.registrar(usuario, ENTIDAD_DEVOLUCION, devolucion.getId(), "REGISTRAR", codigo + " por S/ " + monto);
        return ComprasMapper.devolucion(devolucion);
    }

    @Transactional
    public DevolucionResponse aplicarCredito(UsuarioPrincipal actor, UUID devolucionId, UUID comprobanteId) {
        DevolucionProveedor devolucion = devolucionRepository
                .findById(devolucionId)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución", "id", devolucionId));
        if (devolucion.getComprobante() != null) {
            throw new ConflictException("La devolución " + devolucion.getCodigo() + " ya tiene un crédito aplicado");
        }
        comprobanteRepository
                .findParaActualizar(comprobanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", "id", comprobanteId));
        ComprobanteProveedor comprobante =
                comprobanteRepository.findConDetalle(comprobanteId).orElseThrow();
        if (!comprobante.getProveedor().getId().equals(devolucion.getProveedor().getId())) {
            throw new ConflictException("El comprobante es de otro proveedor");
        }
        if (comprobante.getEstado() == EstadoComprobanteProveedor.ANULADO) {
            throw new ConflictException("El comprobante está anulado");
        }
        if (devolucion.getMonto().compareTo(comprobante.saldo()) > 0) {
            throw new ConflictException("La devolución excede el saldo del comprobante");
        }
        comprobante.setMontoCredito(comprobante.getMontoCredito().add(devolucion.getMonto()));
        if (comprobante.saldo().signum() == 0) {
            comprobante.setEstado(EstadoComprobanteProveedor.PAGADO);
        }
        devolucion.setComprobante(comprobante);
        auditor.registrar(
                usuario(actor),
                ENTIDAD_DEVOLUCION,
                devolucionId,
                "APLICAR_CREDITO",
                devolucion.getCodigo() + " en " + comprobante.numeroCompleto());
        return ComprasMapper.devolucion(devolucion);
    }

    private void verificarSegregacion(UsuarioPrincipal actor, OrdenCompra orden) {
        if (orden.getAprobadaPor() != null && orden.getAprobadaPor().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Quien aprueba una orden no puede recibirla");
        }
        if (orden.getRequisicion() != null
                && orden.getRequisicion().getSolicitante().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Quien solicita una requisición no puede recibir su orden");
        }
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
