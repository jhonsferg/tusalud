package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CitasPorEstadoResponse;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final int DIAS_RANGO_POR_DEFECTO = 30;

    private final CitaRepository citaRepository;

    @Override
    public List<CitasPorEstadoResponse> contarCitasPorEstado() {
        return citaRepository.contarPorEstado();
    }

    @Override
    public List<CitaResponse> listarCitas(LocalDate inicio, LocalDate fin) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = inicio != null ? inicio : hoy.minusDays(DIAS_RANGO_POR_DEFECTO);
        LocalDate hasta = fin != null ? fin : hoy.plusDays(DIAS_RANGO_POR_DEFECTO);
        if (desde.isAfter(hasta)) {
            throw new BusinessLogicException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return citaRepository.findAllByRango(desde.atStartOfDay(), hasta.atTime(LocalTime.MAX)).stream()
                .map(CitaResponse::from)
                .toList();
    }
}
