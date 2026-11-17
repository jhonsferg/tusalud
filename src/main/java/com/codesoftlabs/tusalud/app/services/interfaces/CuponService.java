package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CuponRequest;
import com.codesoftlabs.tusalud.app.dto.response.CuponResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValidacionCuponResponse;
import com.codesoftlabs.tusalud.app.entities.Cupon;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CuponService {

    List<CuponResponse> listar();

    CuponResponse crear(CuponRequest request);

    CuponResponse actualizar(UUID id, CuponRequest request);

    void eliminar(UUID id);

    ValidacionCuponResponse validar(String codigo, BigDecimal monto);

    Cupon consumir(String codigo);
}
