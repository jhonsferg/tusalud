package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;

public interface CitaService {

    CitaResponse crearCita(CitaCreateRequest request, UsuarioPrincipal paciente);

    List<CitaResponse> listarMisCitas(UsuarioPrincipal paciente, LocalDate inicio, LocalDate fin);

    CitaResponse obtenerCita(Long citaId, UsuarioPrincipal actor);

    CitaResponse cancelarCita(Long citaId, UsuarioPrincipal actor);

    CitaResponse confirmarCita(Long citaId, UsuarioPrincipal actor);

    CitaResponse atenderCita(Long citaId, UsuarioPrincipal medico);

    List<CitaResponse> agendaMedico(UsuarioPrincipal medico, LocalDate fecha);

    DisponibilidadResponse consultarDisponibilidad(Long medicoId, LocalDate fecha);
}
