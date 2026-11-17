package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.TurnoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaHoyResponse;
import com.codesoftlabs.tusalud.app.dto.response.PantallaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecepcionResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;

public interface RecepcionService {

    List<CitaHoyResponse> citasHoy(String q);

    TurnoResponse registrarLlegada(Long citaId);

    TurnoResponse crearTurno(TurnoCreateRequest request);

    List<TurnoResponse> cola(EstadoTurno estado, Long medicoId, UsuarioPrincipal actor);

    TurnoResponse llamar(Long turnoId, UsuarioPrincipal actor);

    TurnoResponse iniciar(Long turnoId, UsuarioPrincipal actor);

    TurnoResponse finalizar(Long turnoId, UsuarioPrincipal actor);

    TurnoResponse marcarAusente(Long turnoId, UsuarioPrincipal actor);

    TurnoResponse cancelar(Long turnoId, UsuarioPrincipal actor);

    List<PantallaTurnoResponse> pantalla();

    RecepcionResumenResponse resumen();
}
