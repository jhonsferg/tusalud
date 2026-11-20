package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.OrdenCompraRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.OrdenItemRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ConfiguracionCompras;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenCompraDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenCompraResumen;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenItemResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionResumen;
import com.codesoftlabs.tusalud.app.entities.Cotizacion;
import com.codesoftlabs.tusalud.app.entities.CotizacionItem;
import com.codesoftlabs.tusalud.app.entities.OrdenCompra;
import com.codesoftlabs.tusalud.app.entities.OrdenCompraItem;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.Proveedor;
import com.codesoftlabs.tusalud.app.entities.Requisicion;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoCotizacion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.NivelAprobacion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ComprobanteProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.CotizacionRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenCompraRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import com.codesoftlabs.tusalud.app.repositories.ProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.RecepcionCompraRepository;
import com.codesoftlabs.tusalud.app.repositories.RequisicionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenCompraService {

    private static final int LIMITE = 200;
    private static final int DIAS_POR_DEFECTO = 90;
    private static final String ENTIDAD = "ORDEN_COMPRA";
    private static final String ENLACE = "/finanzas/compras/ordenes/";

    private final OrdenCompraRepository ordenRepository;
    private final RequisicionRepository requisicionRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final RecepcionCompraRepository recepcionRepository;
    private final ComprobanteProveedorRepository comprobanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComprasSecuencias secuencias;
    private final ComprasAuditor auditor;
    private final ComprasAvisos avisos;

    @Value("${app.compras.limite-finanzas:5000.00}")
    private BigDecimal limiteFinanzas;

    public ConfiguracionCompras configuracion() {
        return new ConfiguracionCompras(limiteFinanzas, OrdenCompra.TASA_IGV);
    }

    public List<OrdenCompraResumen> listar(
            EstadoOrdenCompra estado, UUID proveedorId, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        return ordenRepository
                .buscar(
                        estado,
                        proveedorId,
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay(),
                        PageRequest.of(0, LIMITE))
                .stream()
                .map(ComprasMapper::ordenResumen)
                .toList();
    }

    public OrdenCompraDetalle obtener(UUID id) {
        return detalle(orden(id));
    }

    @Transactional
    public OrdenCompraDetalle crear(UsuarioPrincipal actor, OrdenCompraRequest request) {
        Usuario usuario = usuario(actor);
        Proveedor proveedor = proveedorActivo(request.proveedorId());
        Requisicion requisicion = null;
        Cotizacion cotizacion = null;
        if (request.cotizacionId() != null) {
            cotizacion = cotizacionRepository
                    .findConDetalle(request.cotizacionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cotización", "id", request.cotizacionId()));
            if (cotizacion.getEstado() != EstadoCotizacion.SELECCIONADA) {
                throw new ConflictException("La cotización no fue seleccionada");
            }
            if (!cotizacion.getProveedor().getId().equals(proveedor.getId())) {
                throw new ConflictException("El proveedor no coincide con el de la cotización");
            }
            requisicion = cotizacion.getRequisicion();
        }
        if (request.requisicionId() != null) {
            if (requisicion != null && !requisicion.getId().equals(request.requisicionId())) {
                throw new ConflictException("La cotización no pertenece a la requisición indicada");
            }
            requisicion = requisicionRepository
                    .findParaActualizar(request.requisicionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requisición", "id", request.requisicionId()));
        }
        if (requisicion != null) {
            if (requisicion.getEstado() != EstadoRequisicion.ENVIADA
                    && requisicion.getEstado() != EstadoRequisicion.EN_COTIZACION
                    && requisicion.getEstado() != EstadoRequisicion.ATENDIDA) {
                throw new ConflictException("La requisición " + requisicion.getCodigo() + " no admite órdenes");
            }
            if (requisicion.getEstado() == EstadoRequisicion.ENVIADA) {
                requisicion.setEstado(EstadoRequisicion.EN_COTIZACION);
            }
        }
        OrdenCompra orden = OrdenCompra.builder()
                .estado(EstadoOrdenCompra.BORRADOR)
                .proveedor(proveedor)
                .requisicion(requisicion)
                .cotizacion(cotizacion)
                .creadaPor(usuario)
                .afectaIgv(request.afectaIgv() == null || request.afectaIgv())
                .condicionPagoDias(
                        request.condicionPagoDias() == null
                                ? proveedor.getCondicionPagoDias()
                                : request.condicionPagoDias())
                .fechaEntregaEstimada(entrega(request, proveedor))
                .observaciones(limpiar(request.observaciones()))
                .creadaEn(LocalDateTime.now())
                .build();
        cargarItems(orden, request.items());
        orden.recalcular();
        ordenRepository.save(orden);
        auditor.registrar(
                usuario, ENTIDAD, orden.getId(), "CREAR", proveedor.getRazonSocial() + " " + orden.getTotal());
        return detalle(orden);
    }

    @Transactional
    public OrdenCompraDetalle desdeCotizacion(UsuarioPrincipal actor, UUID cotizacionId) {
        Cotizacion cotizacion = cotizacionRepository
                .findConDetalle(cotizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización", "id", cotizacionId));
        List<OrdenItemRequest> items = cotizacion.getItems().stream()
                .map((CotizacionItem i) ->
                        new OrdenItemRequest(i.getProducto().getId(), i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        return crear(
                actor,
                new OrdenCompraRequest(
                        cotizacion.getProveedor().getId(),
                        cotizacion.getRequisicion().getId(),
                        cotizacionId,
                        true,
                        cotizacion.getCondicionPagoDias(),
                        cotizacion.getPlazoEntregaDias() == null
                                ? null
                                : LocalDate.now().plusDays(cotizacion.getPlazoEntregaDias()),
                        null,
                        items));
    }

    @Transactional
    public OrdenCompraDetalle actualizar(UsuarioPrincipal actor, UUID id, OrdenCompraRequest request) {
        OrdenCompra orden = ordenParaActualizar(id);
        soloCreador(actor, orden);
        exigirEstado(orden, "solo se edita en borrador", EstadoOrdenCompra.BORRADOR);
        Proveedor proveedor = proveedorActivo(request.proveedorId());
        orden.setProveedor(proveedor);
        orden.setAfectaIgv(request.afectaIgv() == null || request.afectaIgv());
        orden.setCondicionPagoDias(
                request.condicionPagoDias() == null ? proveedor.getCondicionPagoDias() : request.condicionPagoDias());
        orden.setFechaEntregaEstimada(entrega(request, proveedor));
        orden.setObservaciones(limpiar(request.observaciones()));
        orden.limpiarItems();
        ordenRepository.flush();
        cargarItems(orden, request.items());
        orden.recalcular();
        auditor.registrar(
                usuario(actor), ENTIDAD, id, "EDITAR", orden.getTotal().toPlainString());
        return detalle(orden);
    }

    @Transactional
    public OrdenCompraDetalle enviarAprobacion(UsuarioPrincipal actor, UUID id) {
        OrdenCompra orden = ordenParaActualizar(id);
        soloCreador(actor, orden);
        exigirEstado(orden, "ya fue enviada a aprobación", EstadoOrdenCompra.BORRADOR);
        if (orden.getItems().isEmpty() || orden.getTotal().signum() <= 0) {
            throw new ConflictException("La orden no tiene productos");
        }
        proveedorActivo(orden.getProveedor().getId());
        NivelAprobacion nivel =
                orden.getTotal().compareTo(limiteFinanzas) > 0 ? NivelAprobacion.GERENCIA : NivelAprobacion.FINANZAS;
        orden.setNivelAprobacion(nivel);
        orden.setEstado(EstadoOrdenCompra.PENDIENTE_APROBACION);
        orden.setEnviadaEn(LocalDateTime.now());
        Usuario usuario = usuario(actor);
        auditor.registrar(usuario, ENTIDAD, id, "ENVIAR_APROBACION", nivel.name() + " " + orden.getTotal());
        avisos.avisarRol(
                nivel == NivelAprobacion.GERENCIA ? RolNombre.ROLE_GERENCIA : RolNombre.ROLE_FINANZAS,
                actor.getId(),
                TipoNotificacion.ORDEN_COMPRA_POR_APROBAR,
                "Orden de compra por aprobar",
                "Orden de " + orden.getProveedor().getRazonSocial() + " por S/ " + orden.getTotal() + " creada por "
                        + usuario.getNombreCompleto() + ".",
                ENLACE + id);
        return detalle(orden);
    }

    @Transactional
    public OrdenCompraDetalle aprobar(UsuarioPrincipal actor, UUID id) {
        OrdenCompra orden = ordenParaActualizar(id);
        exigirEstado(orden, "no está pendiente de aprobación", EstadoOrdenCompra.PENDIENTE_APROBACION);
        exigirAprobador(actor, orden);
        Usuario usuario = usuario(actor);
        int anio = LocalDate.now().getYear();
        int numero = secuencias.siguiente(ComprasSecuencias.ORDEN, anio);
        orden.setAnio(anio);
        orden.setNumero(numero);
        orden.setCodigo(ComprasSecuencias.formatear(ComprasSecuencias.ORDEN, anio, numero));
        orden.setEstado(EstadoOrdenCompra.APROBADA);
        orden.setAprobadaPor(usuario);
        orden.setAprobadaEn(LocalDateTime.now());
        orden.setHash(hash(orden));
        if (orden.getRequisicion() != null) {
            orden.getRequisicion().setEstado(EstadoRequisicion.ATENDIDA);
            orden.getRequisicion().setResueltaEn(LocalDateTime.now());
            orden.getRequisicion().setResueltaPor(usuario);
        }
        auditor.registrar(usuario, ENTIDAD, id, "APROBAR", orden.getCodigo());
        avisos.avisarUsuario(
                orden.getCreadaPor(),
                TipoNotificacion.ORDEN_COMPRA_RESUELTA,
                "Orden de compra aprobada",
                "La orden " + orden.getCodigo() + " fue aprobada por " + usuario.getNombreCompleto() + ".",
                "/almacen/compras/ordenes/" + id);
        return detalle(orden);
    }

    @Transactional
    public OrdenCompraDetalle rechazar(UsuarioPrincipal actor, UUID id, String motivo) {
        OrdenCompra orden = ordenParaActualizar(id);
        exigirEstado(orden, "no está pendiente de aprobación", EstadoOrdenCompra.PENDIENTE_APROBACION);
        exigirAprobador(actor, orden);
        Usuario usuario = usuario(actor);
        orden.setEstado(EstadoOrdenCompra.RECHAZADA);
        orden.setMotivoResolucion(motivo.strip());
        auditor.registrar(usuario, ENTIDAD, id, "RECHAZAR", motivo);
        reabrirRequisicion(orden);
        avisos.avisarUsuario(
                orden.getCreadaPor(),
                TipoNotificacion.ORDEN_COMPRA_RESUELTA,
                "Orden de compra rechazada",
                "La orden de " + orden.getProveedor().getRazonSocial() + " fue rechazada: " + motivo.strip(),
                "/almacen/compras/ordenes/" + id);
        return detalle(orden);
    }

    @Transactional
    public OrdenCompraDetalle anular(UsuarioPrincipal actor, UUID id, String motivo) {
        OrdenCompra orden = ordenParaActualizar(id);
        exigirEstado(
                orden,
                "no se puede anular en su estado actual",
                EstadoOrdenCompra.BORRADOR,
                EstadoOrdenCompra.PENDIENTE_APROBACION,
                EstadoOrdenCompra.APROBADA);
        boolean conRecepciones =
                orden.getItems().stream().anyMatch(i -> i.getCantidadRecibida() > 0 || i.getCantidadRechazada() > 0);
        if (conRecepciones) {
            throw new ConflictException("La orden ya tiene recepciones, ciérrela en lugar de anularla");
        }
        Usuario usuario = usuario(actor);
        orden.setEstado(EstadoOrdenCompra.ANULADA);
        orden.setAnuladaEn(LocalDateTime.now());
        orden.setAnuladaPor(usuario);
        orden.setMotivoResolucion(motivo.strip());
        auditor.registrar(usuario, ENTIDAD, id, "ANULAR", motivo);
        reabrirRequisicion(orden);
        return detalle(orden);
    }

    @Transactional
    public OrdenCompraDetalle cerrar(UsuarioPrincipal actor, UUID id, String motivo) {
        OrdenCompra orden = ordenParaActualizar(id);
        exigirEstado(orden, "solo se cierra una orden con recepción parcial", EstadoOrdenCompra.PARCIAL);
        Usuario usuario = usuario(actor);
        orden.setEstado(EstadoOrdenCompra.CERRADA);
        orden.setCerradaEn(LocalDateTime.now());
        orden.setMotivoResolucion(motivo.strip());
        auditor.registrar(usuario, ENTIDAD, id, "CERRAR", motivo);
        return detalle(orden);
    }

    OrdenCompra ordenParaActualizar(UUID id) {
        ordenRepository
                .findParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra", "id", id));
        return orden(id);
    }

    OrdenCompra orden(UUID id) {
        return ordenRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra", "id", id));
    }

    OrdenCompraDetalle detalle(OrdenCompra orden) {
        List<OrdenItemResponse> items =
                orden.getItems().stream().map(ComprasMapper::ordenItem).toList();
        List<RecepcionResumen> recepciones = recepcionRepository
                .buscar(
                        orden.getId(),
                        false,
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.now().plusDays(1),
                        PageRequest.of(0, LIMITE))
                .stream()
                .map(ComprasMapper::recepcionResumen)
                .toList();
        return new OrdenCompraDetalle(
                ComprasMapper.ordenResumen(orden),
                orden.getProveedor().getId(),
                orden.getProveedor().getRuc(),
                orden.getRequisicion() == null ? null : orden.getRequisicion().getId(),
                orden.getCotizacion() == null ? null : orden.getCotizacion().getId(),
                orden.isAfectaIgv(),
                orden.getSubtotal(),
                orden.getIgv(),
                orden.getTotal(),
                orden.getCondicionPagoDias(),
                orden.getFechaEntregaEstimada(),
                orden.getObservaciones(),
                orden.getHash(),
                orden.getAprobadaPor() == null ? null : orden.getAprobadaPor().getNombreCompleto(),
                orden.getMotivoResolucion(),
                orden.getEnviadaEn(),
                orden.getCerradaEn(),
                orden.getAnuladaEn(),
                comprobanteRepository.totalFacturadoDeOrden(orden.getId()),
                items,
                recepciones);
    }

    private void cargarItems(OrdenCompra orden, List<OrdenItemRequest> items) {
        Set<UUID> vistos = new HashSet<>();
        for (OrdenItemRequest item : items) {
            if (!vistos.add(item.productoId())) {
                throw new ConflictException("El producto está repetido en la orden");
            }
            Producto producto = productoRepository
                    .findById(item.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", item.productoId()));
            if (!producto.isActivo()) {
                throw new ConflictException("El producto " + producto.getNombre() + " está inactivo");
            }
            orden.agregarItem(OrdenCompraItem.builder()
                    .producto(producto)
                    .cantidad(item.cantidad())
                    .precioUnitario(item.precioUnitario())
                    .subtotal(item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad())))
                    .build());
        }
    }

    private void exigirAprobador(UsuarioPrincipal actor, OrdenCompra orden) {
        boolean gerencia = actor.getRoles().contains(RolNombre.ROLE_GERENCIA.name());
        boolean finanzas = actor.getRoles().contains(RolNombre.ROLE_FINANZAS.name());
        if (orden.getNivelAprobacion() == NivelAprobacion.GERENCIA && !gerencia) {
            throw new ForbiddenOperationException(
                    "Una orden mayor a S/ " + limiteFinanzas + " solo la resuelve la gerencia");
        }
        if (!gerencia && !finanzas) {
            throw new ForbiddenOperationException("No tiene facultad para resolver órdenes de compra");
        }
        if (orden.getCreadaPor().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Quien crea una orden de compra no puede resolverla");
        }
        if (orden.getRequisicion() != null
                && orden.getRequisicion().getSolicitante().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Quien solicita una requisición no puede aprobar su orden");
        }
    }

    private void reabrirRequisicion(OrdenCompra orden) {
        Requisicion requisicion = orden.getRequisicion();
        if (requisicion == null || requisicion.getEstado() != EstadoRequisicion.ATENDIDA) {
            return;
        }
        boolean otraAprobada = ordenRepository.findByRequisicionId(requisicion.getId()).stream()
                .anyMatch(o -> !o.getId().equals(orden.getId()) && o.getEstado().aprobada());
        if (!otraAprobada) {
            requisicion.setEstado(EstadoRequisicion.EN_COTIZACION);
        }
    }

    private void soloCreador(UsuarioPrincipal actor, OrdenCompra orden) {
        if (!orden.getCreadaPor().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo quien creó la orden puede modificarla");
        }
    }

    private void exigirEstado(OrdenCompra orden, String detalle, EstadoOrdenCompra... permitidos) {
        for (EstadoOrdenCompra estado : permitidos) {
            if (orden.getEstado() == estado) {
                return;
            }
        }
        throw new ConflictException("La orden " + orden.codigoVisible() + " " + detalle);
    }

    private Proveedor proveedorActivo(UUID id) {
        Proveedor proveedor = proveedorRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", id));
        if (!proveedor.isActivo()) {
            throw new ConflictException("El proveedor " + proveedor.getRazonSocial() + " está inactivo");
        }
        return proveedor;
    }

    private LocalDate entrega(OrdenCompraRequest request, Proveedor proveedor) {
        if (request.fechaEntregaEstimada() != null) {
            return request.fechaEntregaEstimada();
        }
        return proveedor.getPlazoEntregaDias() == null
                ? null
                : LocalDate.now().plusDays(proveedor.getPlazoEntregaDias());
    }

    private Usuario usuario(UsuarioPrincipal actor) {
        return usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
    }

    private static String hash(OrdenCompra orden) {
        StringBuilder base = new StringBuilder()
                .append(orden.getCodigo())
                .append('|')
                .append(orden.getProveedor().getRuc())
                .append('|')
                .append(orden.getTotal().toPlainString())
                .append('|')
                .append(orden.isAfectaIgv());
        orden.getItems()
                .forEach(i -> base.append('|')
                        .append(i.getProducto().getId())
                        .append(':')
                        .append(i.getCantidad())
                        .append(':')
                        .append(i.getPrecioUnitario().toPlainString()));
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(base.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
