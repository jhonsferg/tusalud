package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.TurnoPersonalRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlantillaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.SemanaTurnosResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoPersonalResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TurnoPersonalService {

    List<PlantillaTurnoResponse> plantillas();

    TurnoPersonalResponse asignar(TurnoPersonalRequest request, UsuarioPrincipal actor);

    void eliminar(UUID id, UsuarioPrincipal actor);

    List<TurnoPersonalResponse> listar(LocalDate desde, LocalDate hasta, AreaLaboral area, UUID empleadoId);

    SemanaTurnosResponse semana(LocalDate fecha, AreaLaboral area);

    List<TurnoPersonalResponse> misTurnos(UsuarioPrincipal actor, LocalDate desde, LocalDate hasta);
}
