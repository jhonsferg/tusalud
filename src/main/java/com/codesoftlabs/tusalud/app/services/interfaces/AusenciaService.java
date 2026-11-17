package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.AusenciaRequest;
import com.codesoftlabs.tusalud.app.dto.response.AusenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.MisAusenciasResponse;
import com.codesoftlabs.tusalud.app.dto.response.SaldoVacacionesResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AusenciaService {

    AusenciaResponse solicitar(UsuarioPrincipal actor, AusenciaRequest request);

    MisAusenciasResponse misAusencias(UsuarioPrincipal actor);

    AusenciaResponse cancelarPropia(UsuarioPrincipal actor, UUID id);

    List<AusenciaResponse> listar(
            EstadoAusencia estado, AreaLaboral area, LocalDate desde, LocalDate hasta, UsuarioPrincipal actor);

    AusenciaResponse aprobar(UUID id, String motivo, UsuarioPrincipal actor);

    AusenciaResponse rechazar(UUID id, String motivo, UsuarioPrincipal actor);

    AusenciaResponse cancelar(UUID id, UsuarioPrincipal actor);

    SaldoVacacionesResponse saldo(UUID empleadoId);
}
