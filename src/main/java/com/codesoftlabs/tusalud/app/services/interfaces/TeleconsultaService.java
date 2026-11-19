package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.ConsentimientoTelemedicinaResponse;
import com.codesoftlabs.tusalud.app.dto.response.IceServersResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenPacienteTeleconsultaResponse;
import com.codesoftlabs.tusalud.app.dto.response.TeleconsultaResponse;
import com.codesoftlabs.tusalud.app.dto.response.TicketTeleconsultaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TeleconsultaService {

    void crearParaCita(Cita cita);

    List<TeleconsultaResponse> listarMias(UsuarioPrincipal actor);

    List<TeleconsultaResponse> listarDelMedico(UsuarioPrincipal actor, LocalDate desde);

    TeleconsultaResponse obtener(UUID id, UsuarioPrincipal actor);

    ConsentimientoTelemedicinaResponse consentimiento(UsuarioPrincipal actor);

    ConsentimientoTelemedicinaResponse aceptarConsentimiento(UsuarioPrincipal actor, String version);

    TeleconsultaResponse abrir(UUID id, UsuarioPrincipal actor);

    TicketTeleconsultaResponse emitirTicket(UUID id, UsuarioPrincipal actor);

    IceServersResponse ice(UUID id, UsuarioPrincipal actor);

    TeleconsultaResponse finalizar(UUID id, UsuarioPrincipal actor);

    ResumenPacienteTeleconsultaResponse resumenPaciente(UUID id, UsuarioPrincipal actor);

    IngresoSala alConectar(UUID teleconsultaId, UUID usuarioId);

    void alAdmitir(UUID teleconsultaId);

    void alFinalizarDesdeSala(UUID teleconsultaId);

    void alCalidad(UUID teleconsultaId, int reconexiones, String candidato);

    record IngresoSala(String rol, boolean enCurso) {}
}
