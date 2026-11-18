package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CierreCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.CuentaPorCobrarResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteIngresosResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenCajaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SesionCajaResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CajaService {

    SesionCajaResponse abrir(UsuarioPrincipal actor, AperturaCajaRequest request);

    SesionCajaResponse actual(UsuarioPrincipal actor);

    SesionCajaResponse cerrar(UsuarioPrincipal actor, CierreCajaRequest request);

    List<SesionCajaResponse> historial(UsuarioPrincipal actor);

    ResumenCajaResponse resumen(UsuarioPrincipal actor);

    ComprobanteResponse cobrar(UsuarioPrincipal actor, CobroRequest request);

    com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.DesgloseResponse desglosar(
            UsuarioPrincipal actor, com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.DesgloseRequest request);

    List<CuentaPorCobrarResponse> pendientes(String q);

    List<ComprobanteResponse> comprobantes(LocalDate desde, LocalDate hasta, String q, EstadoComprobante estado);

    ComprobanteResponse obtener(UUID id);

    ComprobanteResponse anular(UsuarioPrincipal actor, UUID id, String motivo);

    ReporteIngresosResponse reporteIngresos(LocalDate desde, LocalDate hasta);
}
