package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.PagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PagoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Pago;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoService {

    PagoResponse pagarCita(Long citaId, PagoRequest request, UsuarioPrincipal actor);

    Pago registrarPagoBilletera(
            Cita cita, String descripcion, BigDecimal monto, BigDecimal descuento, String codigoCupon);

    List<PagoResponse> listarMisPagos(UsuarioPrincipal actor);

    PagoResponse obtenerMiPago(UsuarioPrincipal actor, Long id);

    List<PagoResponse> listarTodos(LocalDate inicio, LocalDate fin);
}
