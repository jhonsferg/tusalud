package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.AjusteRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SalidaRequest;
import com.codesoftlabs.tusalud.app.dto.response.AlertasInventarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoResponse;
import com.codesoftlabs.tusalud.app.dto.response.LoteResponse;
import com.codesoftlabs.tusalud.app.dto.response.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenAlmacenResponse;
import com.codesoftlabs.tusalud.app.entities.Ingreso;
import com.codesoftlabs.tusalud.app.entities.Lote;
import com.codesoftlabs.tusalud.app.entities.MovimientoInventario;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.Proveedor;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.IngresoRepository;
import com.codesoftlabs.tusalud.app.repositories.LoteRepository;
import com.codesoftlabs.tusalud.app.repositories.MovimientoInventarioRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import com.codesoftlabs.tusalud.app.repositories.ProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlmacenServiceImpl implements AlmacenService {

    private static final int LIMITE_MOVIMIENTOS = 200;
    private static final int LIMITE_INGRESOS = 100;
    private static final int DIAS_POR_DEFECTO = 30;

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final LoteRepository loteRepository;
    private final IngresoRepository ingresoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final ProductoResponseMapper mapper;

    @Override
    @Transactional
    public IngresoResponse registrarIngreso(UsuarioPrincipal actor, IngresoRequest request) {
        Usuario usuario = usuario(actor);
        Proveedor proveedor = null;
        if (request.proveedorId() != null) {
            proveedor = proveedorRepository
                    .findById(request.proveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", request.proveedorId()));
            if (!proveedor.isActivo()) {
                throw new ConflictException("El proveedor " + proveedor.getRazonSocial() + " está inactivo");
            }
        }
        Set<String> vistos = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (IngresoItemRequest item : request.items()) {
            String clave = item.productoId() + "|" + item.numeroLote().trim().toUpperCase();
            if (!vistos.add(clave)) {
                throw new ConflictException("El lote " + item.numeroLote() + " está repetido en el ingreso");
            }
            total = total.add(item.costoUnitario().multiply(BigDecimal.valueOf(item.cantidad())));
        }
        Ingreso ingreso = ingresoRepository.save(Ingreso.builder()
                .fecha(LocalDateTime.now())
                .documento(limpiar(request.documento()))
                .proveedor(proveedor)
                .observaciones(limpiar(request.observaciones()))
                .total(total)
                .usuario(usuario)
                .build());
        String referencia = String.format("ING-%06d", ingresoRepository.count());
        LocalDate hoy = LocalDate.now();
        List<LoteResponse> lotes = new java.util.ArrayList<>();
        for (IngresoItemRequest item : request.items()) {
            Producto producto = productoRepository
                    .findById(item.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", item.productoId()));
            if (!producto.isActivo()) {
                throw new ConflictException("El producto " + producto.getNombre() + " está inactivo");
            }
            String numero = item.numeroLote().trim();
            if (loteRepository.existsByProductoIdAndNumeroLote(producto.getId(), numero)) {
                throw new ConflictException(
                        "Ya existe el lote " + numero + " para el producto " + producto.getNombre());
            }
            Lote lote = loteRepository.save(Lote.builder()
                    .producto(producto)
                    .ingreso(ingreso)
                    .proveedor(proveedor)
                    .numeroLote(numero)
                    .fechaVencimiento(item.fechaVencimiento())
                    .cantidadInicial(item.cantidad())
                    .cantidadActual(item.cantidad())
                    .costoUnitario(item.costoUnitario())
                    .ubicacion(limpiar(item.ubicacion()))
                    .build());
            movimientoRepository.save(MovimientoInventario.builder()
                    .fecha(LocalDateTime.now())
                    .tipo(MotivoMovimiento.COMPRA.tipo())
                    .motivo(MotivoMovimiento.COMPRA)
                    .producto(producto)
                    .lote(lote)
                    .cantidad(item.cantidad())
                    .usuario(usuario)
                    .referencia(referencia)
                    .build());
            lotes.add(LoteResponse.from(lote, hoy));
        }
        return IngresoResponse.from(ingreso, lotes);
    }

    @Override
    public List<IngresoResponse> listarIngresos() {
        List<Ingreso> ingresos = ingresoRepository.recientes(PageRequest.of(0, LIMITE_INGRESOS));
        if (ingresos.isEmpty()) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now();
        Map<UUID, List<LoteResponse>> lotes =
                loteRepository
                        .findByIngresoIds(ingresos.stream().map(Ingreso::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(
                                lote -> lote.getIngreso().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(lote -> LoteResponse.from(lote, hoy), Collectors.toList())));
        return ingresos.stream()
                .map(ingreso -> IngresoResponse.from(ingreso, lotes.getOrDefault(ingreso.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public List<MovimientoResponse> registrarSalida(UsuarioPrincipal actor, SalidaRequest request) {
        Usuario usuario = usuario(actor);
        Producto producto = productoRepository
                .findById(request.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", request.productoId()));
        List<MovimientoInventario> movimientos = inventarioService.descontar(
                producto,
                request.cantidad(),
                request.motivo().aMovimiento(),
                null,
                usuario,
                limpiar(request.observaciones()));
        return movimientos.stream().map(MovimientoResponse::from).toList();
    }

    @Override
    @Transactional
    public MovimientoResponse registrarAjuste(UsuarioPrincipal actor, AjusteRequest request) {
        Usuario usuario = usuario(actor);
        Lote lote = loteRepository
                .findByIdParaActualizar(request.loteId())
                .orElseThrow(() -> new ResourceNotFoundException("Lote", "id", request.loteId()));
        if (lote.estaVencido(LocalDate.now())) {
            throw new ConflictException("No se puede ajustar el stock de un lote vencido");
        }
        lote.setCantidadActual(lote.getCantidadActual() + request.cantidad());
        MovimientoInventario movimiento = movimientoRepository.save(MovimientoInventario.builder()
                .fecha(LocalDateTime.now())
                .tipo(MotivoMovimiento.AJUSTE_POSITIVO.tipo())
                .motivo(MotivoMovimiento.AJUSTE_POSITIVO)
                .producto(lote.getProducto())
                .lote(lote)
                .cantidad(request.cantidad())
                .usuario(usuario)
                .observaciones(limpiar(request.observaciones()))
                .build());
        return MovimientoResponse.from(movimiento);
    }

    @Override
    public List<MovimientoResponse> movimientos(
            UUID productoId, TipoMovimiento tipo, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        Pageable limite = PageRequest.of(0, LIMITE_MOVIMIENTOS);
        return movimientoRepository
                .buscar(productoId, tipo, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(), limite)
                .stream()
                .map(MovimientoResponse::from)
                .toList();
    }

    @Override
    public AlertasInventarioResponse alertas() {
        LocalDate hoy = LocalDate.now();
        List<ProductoResponse> bajoStock =
                mapper.aRespuestas(productoRepository.buscar(null, null, true, PageRequest.of(0, 1000))).stream()
                        .filter(ProductoResponse::bajoStock)
                        .toList();
        List<LoteResponse> porVencer = loteRepository.porVencer(hoy, hoy.plusDays(Lote.DIAS_POR_VENCER)).stream()
                .map(lote -> LoteResponse.from(lote, hoy))
                .toList();
        List<LoteResponse> vencidos = loteRepository.vencidosConStock(hoy).stream()
                .map(lote -> LoteResponse.from(lote, hoy))
                .toList();
        return new AlertasInventarioResponse(bajoStock, porVencer, vencidos);
    }

    @Override
    public ResumenAlmacenResponse resumen() {
        AlertasInventarioResponse alertas = alertas();
        return new ResumenAlmacenResponse(
                productoRepository.countByActivoTrue(),
                alertas.bajoStock().size(),
                alertas.porVencer().size(),
                alertas.vencidos().size(),
                loteRepository.valorInventario(LocalDate.now()));
    }

    private Usuario usuario(UsuarioPrincipal actor) {
        return usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
