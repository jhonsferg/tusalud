package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ReprogramarRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadRangoResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CitaService {

    CitaResponse crearCita(CitaCreateRequest request, UsuarioPrincipal paciente);

    List<CitaResponse> listarMisCitas(UsuarioPrincipal paciente, LocalDate inicio, LocalDate fin);

    CitaResponse obtenerCita(UUID citaId, UsuarioPrincipal actor);

    CitaResponse cancelarCita(UUID citaId, String motivo, UsuarioPrincipal actor);

    CitaResponse confirmarCita(UUID citaId, UsuarioPrincipal actor);

    CitaResponse atenderCita(UUID citaId, UsuarioPrincipal medico);

    CitaResponse reprogramarCita(UUID citaId, ReprogramarRequest request, UsuarioPrincipal actor);

    List<CitaResponse> agendaMedico(UsuarioPrincipal medico, LocalDate fecha);

    List<CitaResponse> agendaMedico(UsuarioPrincipal medico, LocalDate fecha, LocalDate inicio, LocalDate fin);

    DisponibilidadResponse consultarDisponibilidad(UUID medicoId, LocalDate fecha);

    List<DisponibilidadRangoResponse> consultarDisponibilidadRango(UUID medicoId, LocalDate inicio, LocalDate fin);
}
