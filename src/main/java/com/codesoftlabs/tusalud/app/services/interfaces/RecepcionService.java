package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.TurnoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaHoyResponse;
import com.codesoftlabs.tusalud.app.dto.response.PantallaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecepcionResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface RecepcionService {

    List<CitaHoyResponse> citasHoy(String q);

    TurnoResponse registrarLlegada(UUID citaId);

    TurnoResponse crearTurno(TurnoCreateRequest request);

    List<TurnoResponse> cola(EstadoTurno estado, UUID medicoId, UsuarioPrincipal actor);

    TurnoResponse llamar(UUID turnoId, UsuarioPrincipal actor);

    TurnoResponse iniciar(UUID turnoId, UsuarioPrincipal actor);

    TurnoResponse finalizar(UUID turnoId, UsuarioPrincipal actor);

    TurnoResponse marcarAusente(UUID turnoId, UsuarioPrincipal actor);

    TurnoResponse cancelar(UUID turnoId, UsuarioPrincipal actor);

    List<PantallaTurnoResponse> pantalla();

    RecepcionResumenResponse resumen();
}
