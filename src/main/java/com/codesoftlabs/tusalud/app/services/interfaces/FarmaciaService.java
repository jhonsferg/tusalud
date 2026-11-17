package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.DispensacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.CatalogoFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DispensacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenFarmaciaResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoReceta;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;

public interface FarmaciaService {

    List<CatalogoFarmaciaResponse> catalogo(String q);

    List<RecetaFarmaciaResponse> recetas(EstadoReceta estado, String q);

    DispensacionResponse dispensar(UsuarioPrincipal actor, DispensacionRequest request);

    List<DispensacionResponse> listar(LocalDate desde, LocalDate hasta, String q);

    DispensacionResponse obtener(Long id);

    DispensacionResponse anular(UsuarioPrincipal actor, Long id);

    List<DispensacionResponse> misDispensaciones(UsuarioPrincipal actor);

    ResumenFarmaciaResponse resumen();
}
