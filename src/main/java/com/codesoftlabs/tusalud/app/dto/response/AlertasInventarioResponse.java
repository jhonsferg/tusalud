package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;

public record AlertasInventarioResponse(
        List<ProductoResponse> bajoStock, List<LoteResponse> porVencer, List<LoteResponse> vencidos) {}
