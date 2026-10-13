package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CitasPorEstadoResponse;
import java.time.LocalDate;
import java.util.List;

public interface AuditoriaService {

    List<CitasPorEstadoResponse> contarCitasPorEstado();

    List<CitaResponse> listarCitas(LocalDate inicio, LocalDate fin);
}
