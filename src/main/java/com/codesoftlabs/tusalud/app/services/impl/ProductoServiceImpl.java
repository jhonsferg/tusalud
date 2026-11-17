package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.ProductoRequest;
import com.codesoftlabs.tusalud.app.dto.response.LoteResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.LoteRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ProductoService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {

    private static final int LIMITE = 300;
    private static final Set<TipoProducto> TIPOS_DE_LABORATORIO = Set.of(TipoProducto.INSUMO, TipoProducto.REACTIVO);

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final ProductoResponseMapper mapper;

    @Override
    public List<ProductoResponse> listar(
            UsuarioPrincipal actor, String q, TipoProducto tipo, Boolean bajoStock, Boolean activo) {
        String termino = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        boolean soloLaboratorio = soloLaboratorio(actor);
        List<Producto> productos = productoRepository.buscar(termino, tipo, activo, PageRequest.of(0, LIMITE)).stream()
                .filter(producto -> !soloLaboratorio || TIPOS_DE_LABORATORIO.contains(producto.getTipo()))
                .toList();
        List<ProductoResponse> respuestas = mapper.aRespuestas(productos);
        if (bajoStock == null) {
            return respuestas;
        }
        return respuestas.stream()
                .filter(respuesta -> respuesta.bajoStock() == bajoStock)
                .toList();
    }

    @Override
    public ProductoResponse obtener(UsuarioPrincipal actor, UUID id) {
        return mapper.aRespuesta(buscarVisible(actor, id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (productoRepository.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un producto con el código " + codigo);
        }
        Producto producto = Producto.builder().codigo(codigo).build();
        aplicar(producto, request);
        return mapper.aRespuesta(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(UUID id, ProductoRequest request) {
        Producto producto =
                productoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
        String codigo = normalizarCodigo(request.codigo());
        if (productoRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ConflictException("Ya existe un producto con el código " + codigo);
        }
        producto.setCodigo(codigo);
        aplicar(producto, request);
        return mapper.aRespuesta(producto);
    }

    @Override
    public List<LoteResponse> lotes(UsuarioPrincipal actor, UUID productoId) {
        buscarVisible(actor, productoId);
        LocalDate hoy = LocalDate.now();
        return loteRepository.findByProductoIdConDetalle(productoId).stream()
                .map(lote -> LoteResponse.from(lote, hoy))
                .toList();
    }

    private Producto buscarVisible(UsuarioPrincipal actor, UUID id) {
        Producto producto =
                productoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
        if (soloLaboratorio(actor) && !TIPOS_DE_LABORATORIO.contains(producto.getTipo())) {
            throw new ResourceNotFoundException("Producto", "id", id);
        }
        return producto;
    }

    private boolean soloLaboratorio(UsuarioPrincipal actor) {
        List<String> roles = actor.getRoles();
        return roles.contains(RolNombre.ROLE_LABORATORIO.name())
                && !roles.contains(RolNombre.ROLE_ALMACEN.name())
                && !roles.contains(RolNombre.ROLE_FARMACIA.name())
                && !roles.contains(RolNombre.ROLE_ADMIN.name());
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private void aplicar(Producto producto, ProductoRequest request) {
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(limpiar(request.descripcion()));
        producto.setTipo(request.tipo());
        producto.setPrincipioActivo(limpiar(request.principioActivo()));
        producto.setPresentacion(limpiar(request.presentacion()));
        producto.setUnidad(request.unidad().trim());
        producto.setPrecioVenta(request.precioVenta());
        producto.setStockMinimo(request.stockMinimo());
        producto.setRequiereReceta(Boolean.TRUE.equals(request.requiereReceta()));
        producto.setActivo(request.activo() == null || request.activo());
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
