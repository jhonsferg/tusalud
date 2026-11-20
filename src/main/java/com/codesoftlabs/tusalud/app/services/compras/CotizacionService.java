package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.CotizacionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.CotizacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ColumnaComparativa;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComparativaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.CotizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.FilaComparativa;
import com.codesoftlabs.tusalud.app.entities.Cotizacion;
import com.codesoftlabs.tusalud.app.entities.CotizacionItem;
import com.codesoftlabs.tusalud.app.entities.Proveedor;
import com.codesoftlabs.tusalud.app.entities.Requisicion;
import com.codesoftlabs.tusalud.app.entities.RequisicionItem;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoCotizacion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CotizacionRepository;
import com.codesoftlabs.tusalud.app.repositories.ProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.RequisicionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CotizacionService {

    private static final String ENTIDAD = "COTIZACION";

    private final CotizacionRepository cotizacionRepository;
    private final RequisicionRepository requisicionRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComprasAuditor auditor;

    public List<CotizacionResponse> listar(UUID requisicionId) {
        existeRequisicion(requisicionId);
        LocalDate hoy = LocalDate.now();
        return cotizacionRepository.deRequisicion(requisicionId).stream()
                .map(c -> ComprasMapper.cotizacion(c, hoy))
                .toList();
    }

    @Transactional
    public CotizacionResponse registrar(UsuarioPrincipal actor, UUID requisicionId, CotizacionRequest request) {
        Requisicion requisicion = requisicionRepository
                .findParaActualizar(requisicionId)
                .orElseThrow(() -> new ResourceNotFoundException("Requisición", "id", requisicionId));
        requisicion = requisicionRepository.findConDetalle(requisicionId).orElseThrow();
        if (requisicion.getEstado() != EstadoRequisicion.ENVIADA
                && requisicion.getEstado() != EstadoRequisicion.EN_COTIZACION) {
            throw new ConflictException("La requisición " + requisicion.getCodigo() + " no admite cotizaciones");
        }
        Proveedor proveedor = proveedorRepository
                .findById(request.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", request.proveedorId()));
        if (!proveedor.isActivo()) {
            throw new ConflictException("El proveedor " + proveedor.getRazonSocial() + " está inactivo");
        }
        if (cotizacionRepository.existsByRequisicionIdAndProveedorId(requisicionId, proveedor.getId())) {
            throw new ConflictException("Ya existe una cotización de " + proveedor.getRazonSocial());
        }
        if (request.vigenciaHasta() != null && request.vigenciaHasta().isBefore(LocalDate.now())) {
            throw new ConflictException("La vigencia de la cotización ya pasó");
        }
        Map<UUID, RequisicionItem> pedidos = new HashMap<>();
        requisicion.getItems().forEach(i -> pedidos.put(i.getProducto().getId(), i));
        Usuario usuario = usuario(actor);
        Cotizacion cotizacion = Cotizacion.builder()
                .requisicion(requisicion)
                .proveedor(proveedor)
                .estado(EstadoCotizacion.VIGENTE)
                .vigenciaHasta(request.vigenciaHasta())
                .plazoEntregaDias(
                        request.plazoEntregaDias() == null
                                ? proveedor.getPlazoEntregaDias()
                                : request.plazoEntregaDias())
                .condicionPagoDias(
                        request.condicionPagoDias() == null
                                ? proveedor.getCondicionPagoDias()
                                : request.condicionPagoDias())
                .observaciones(limpiar(request.observaciones()))
                .total(BigDecimal.ZERO)
                .registradaPor(usuario)
                .creadaEn(LocalDateTime.now())
                .build();
        Set<UUID> vistos = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CotizacionItemRequest item : request.items()) {
            RequisicionItem pedido = pedidos.get(item.productoId());
            if (pedido == null) {
                throw new ConflictException("El producto cotizado no pertenece a la requisición");
            }
            if (!vistos.add(item.productoId())) {
                throw new ConflictException("El producto está repetido en la cotización");
            }
            cotizacion.agregarItem(CotizacionItem.builder()
                    .producto(pedido.getProducto())
                    .cantidad(pedido.getCantidad())
                    .precioUnitario(item.precioUnitario())
                    .build());
            total = total.add(item.precioUnitario().multiply(BigDecimal.valueOf(pedido.getCantidad())));
        }
        cotizacion.setTotal(total);
        cotizacionRepository.save(cotizacion);
        if (requisicion.getEstado() == EstadoRequisicion.ENVIADA) {
            requisicion.setEstado(EstadoRequisicion.EN_COTIZACION);
        }
        auditor.registrar(usuario, ENTIDAD, cotizacion.getId(), "REGISTRAR", proveedor.getRazonSocial() + " " + total);
        return ComprasMapper.cotizacion(cotizacion, LocalDate.now());
    }

    @Transactional
    public CotizacionResponse seleccionar(UsuarioPrincipal actor, UUID cotizacionId) {
        Cotizacion cotizacion = cotizacionRepository
                .findConDetalle(cotizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización", "id", cotizacionId));
        UUID requisicionId = cotizacion.getRequisicion().getId();
        requisicionRepository
                .findParaActualizar(requisicionId)
                .orElseThrow(() -> new ResourceNotFoundException("Requisición", "id", requisicionId));
        if (cotizacion.getRequisicion().getEstado() != EstadoRequisicion.EN_COTIZACION) {
            throw new ConflictException("La requisición no está en cotización");
        }
        if (cotizacion.getVigenciaHasta() != null
                && cotizacion.getVigenciaHasta().isBefore(LocalDate.now())) {
            throw new ConflictException("La cotización ya venció");
        }
        List<Cotizacion> todas = cotizacionRepository.deRequisicion(requisicionId);
        for (Cotizacion otra : todas) {
            otra.setEstado(
                    otra.getId().equals(cotizacionId) ? EstadoCotizacion.SELECCIONADA : EstadoCotizacion.DESCARTADA);
        }
        auditor.registrar(
                usuario(actor),
                ENTIDAD,
                cotizacionId,
                "SELECCIONAR",
                cotizacion.getProveedor().getRazonSocial());
        return ComprasMapper.cotizacion(cotizacion, LocalDate.now());
    }

    public ComparativaResponse comparativa(UUID requisicionId) {
        Requisicion requisicion = requisicionRepository
                .findConDetalle(requisicionId)
                .orElseThrow(() -> new ResourceNotFoundException("Requisición", "id", requisicionId));
        List<Cotizacion> cotizaciones = cotizacionRepository.deRequisicion(requisicionId);
        int pedidos = requisicion.getItems().size();
        BigDecimal mejorTotal = cotizaciones.stream()
                .filter(c -> c.getItems().size() == pedidos)
                .map(Cotizacion::getTotal)
                .min(BigDecimal::compareTo)
                .orElse(null);
        List<ColumnaComparativa> columnas = new ArrayList<>();
        for (Cotizacion c : cotizaciones) {
            boolean cubreTodo = c.getItems().size() == pedidos;
            columnas.add(new ColumnaComparativa(
                    c.getId(),
                    c.getProveedor().getRazonSocial(),
                    c.getEstado(),
                    c.getTotal(),
                    cubreTodo,
                    c.getPlazoEntregaDias(),
                    c.getCondicionPagoDias(),
                    cubreTodo && mejorTotal != null && c.getTotal().compareTo(mejorTotal) == 0));
        }
        List<FilaComparativa> filas = new ArrayList<>();
        for (RequisicionItem pedido : requisicion.getItems()) {
            List<BigDecimal> precios = new ArrayList<>();
            BigDecimal mejor = null;
            UUID mejorId = null;
            for (Cotizacion c : cotizaciones) {
                BigDecimal precio = c.getItems().stream()
                        .filter(i -> i.getProducto()
                                .getId()
                                .equals(pedido.getProducto().getId()))
                        .map(CotizacionItem::getPrecioUnitario)
                        .findFirst()
                        .orElse(null);
                precios.add(precio);
                if (precio != null && (mejor == null || precio.compareTo(mejor) < 0)) {
                    mejor = precio;
                    mejorId = c.getId();
                }
            }
            filas.add(new FilaComparativa(
                    pedido.getProducto().getId(),
                    pedido.getProducto().getNombre(),
                    pedido.getProducto().getUnidad(),
                    pedido.getCantidad(),
                    precios,
                    mejorId));
        }
        return new ComparativaResponse(requisicionId, columnas, filas);
    }

    private void existeRequisicion(UUID id) {
        if (!requisicionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Requisición", "id", id);
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
