package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CompletarEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.EventoCalendarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MoverEventoRequest;
import com.codesoftlabs.tusalud.app.dto.response.EventoCalendarioResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventoCalendarioService {

    List<EventoCalendarioResponse> listar(UsuarioPrincipal actor, LocalDate inicio, LocalDate fin);

    EventoCalendarioResponse crear(UsuarioPrincipal actor, EventoCalendarioRequest request);

    EventoCalendarioResponse actualizar(UsuarioPrincipal actor, UUID id, EventoCalendarioRequest request);

    EventoCalendarioResponse mover(UsuarioPrincipal actor, UUID id, MoverEventoRequest request);

    EventoCalendarioResponse completar(UsuarioPrincipal actor, UUID id, CompletarEventoRequest request);

    List<TipoEvento> tiposPermitidos(UsuarioPrincipal actor);

    void eliminar(UsuarioPrincipal actor, UUID id);
}
