package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CompletarEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.EventoCalendarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MoverEventoRequest;
import com.codesoftlabs.tusalud.app.dto.response.EventoCalendarioResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;

public interface EventoCalendarioService {

    List<EventoCalendarioResponse> listar(UsuarioPrincipal actor, LocalDate inicio, LocalDate fin);

    EventoCalendarioResponse crear(UsuarioPrincipal actor, EventoCalendarioRequest request);

    EventoCalendarioResponse actualizar(UsuarioPrincipal actor, Long id, EventoCalendarioRequest request);

    EventoCalendarioResponse mover(UsuarioPrincipal actor, Long id, MoverEventoRequest request);

    EventoCalendarioResponse completar(UsuarioPrincipal actor, Long id, CompletarEventoRequest request);

    List<TipoEvento> tiposPermitidos(UsuarioPrincipal actor);

    void eliminar(UsuarioPrincipal actor, Long id);
}
