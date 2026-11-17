package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.PagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PagoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;

public interface PagoService {

    PagoResponse pagarCita(Long citaId, PagoRequest request, UsuarioPrincipal actor);

    List<PagoResponse> listarMisPagos(UsuarioPrincipal actor);

    PagoResponse obtenerMiPago(UsuarioPrincipal actor, Long id);

    List<PagoResponse> listarTodos(LocalDate inicio, LocalDate fin);
}
