package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.AjusteRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SalidaRequest;
import com.codesoftlabs.tusalud.app.dto.response.AlertasInventarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoResponse;
import com.codesoftlabs.tusalud.app.dto.response.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenAlmacenResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AlmacenService {

    IngresoResponse registrarIngreso(UsuarioPrincipal actor, IngresoRequest request);

    List<IngresoResponse> listarIngresos();

    List<MovimientoResponse> registrarSalida(UsuarioPrincipal actor, SalidaRequest request);

    MovimientoResponse registrarAjuste(UsuarioPrincipal actor, AjusteRequest request);

    List<MovimientoResponse> movimientos(UUID productoId, TipoMovimiento tipo, LocalDate desde, LocalDate hasta);

    AlertasInventarioResponse alertas();

    ResumenAlmacenResponse resumen();
}
