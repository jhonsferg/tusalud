package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.codesoftlabs.tusalud.app.repositories.HorarioAtencionRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CitaResponseMapper {

    public static final int DURACION_POR_DEFECTO = 30;

    private final HorarioAtencionRepository horarioRepository;

    public CitaResponse mapear(Cita cita) {
        return CitaResponse.from(cita, duracionDe(cita));
    }

    public List<CitaResponse> mapear(Collection<Cita> citas) {
        Map<Long, List<HorarioAtencion>> horariosPorMedico = new HashMap<>();
        return citas.stream()
                .map(cita -> CitaResponse.from(
                        cita,
                        duracion(
                                horariosPorMedico.computeIfAbsent(
                                        cita.getMedico().getId(), horarioRepository::findByMedicoId),
                                cita.getFechaHora())))
                .toList();
    }

    public int duracionDe(Cita cita) {
        return duracion(horarioRepository.findByMedicoId(cita.getMedico().getId()), cita.getFechaHora());
    }

    private static int duracion(List<HorarioAtencion> horarios, LocalDateTime fechaHora) {
        return horarios.stream()
                .filter(h -> h.getDiaSemana() == fechaHora.getDayOfWeek() && h.admiteCitaEn(fechaHora.toLocalTime()))
                .map(HorarioAtencion::getDuracionCitaMinutos)
                .findFirst()
                .orElse(DURACION_POR_DEFECTO);
    }
}
