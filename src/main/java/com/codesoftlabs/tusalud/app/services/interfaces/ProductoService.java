package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.ProductoRequest;
import com.codesoftlabs.tusalud.app.dto.response.LoteResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface ProductoService {

    List<ProductoResponse> listar(
            UsuarioPrincipal actor, String q, TipoProducto tipo, Boolean bajoStock, Boolean activo);

    ProductoResponse obtener(UsuarioPrincipal actor, UUID id);

    ProductoResponse crear(ProductoRequest request);

    ProductoResponse actualizar(UUID id, ProductoRequest request);

    List<LoteResponse> lotes(UsuarioPrincipal actor, UUID productoId);
}
