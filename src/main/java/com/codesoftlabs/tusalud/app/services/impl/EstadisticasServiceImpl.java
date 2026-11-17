package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.EstadisticasResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoMesResponse;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.EspecialidadRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.PagoRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.EstadisticasService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstadisticasServiceImpl implements EstadisticasService {

    private static final int MESES_HISTORICO = 6;
    private static final int TOP_MEDICOS = 5;

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final CitaRepository citaRepository;
    private final PagoRepository pagoRepository;

    @Override
    public EstadisticasResponse obtener() {
        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.from(hoy);

        List<IngresoMesResponse> ingresosPorMes = new ArrayList<>();
        for (int atras = MESES_HISTORICO - 1; atras >= 0; atras--) {
            YearMonth mes = mesActual.minusMonths(atras);
            ingresosPorMes.add(new IngresoMesResponse(
                    mes.toString(), sumar(mes.atDay(1), mes.plusMonths(1).atDay(1))));
        }

        return new EstadisticasResponse(
                pacienteRepository.count(),
                medicoRepository.count(),
                especialidadRepository.count(),
                citaRepository.contarActivasEntre(
                        hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay()),
                citaRepository.contarPendientes(),
                sumar(mesActual.atDay(1), mesActual.plusMonths(1).atDay(1)),
                sumar(LocalDate.of(2000, 1, 1), LocalDate.of(2999, 1, 1)),
                citaRepository.topMedicosAtendidos(PageRequest.of(0, TOP_MEDICOS)),
                ingresosPorMes,
                citaRepository.contarPorEspecialidad());
    }

    private BigDecimal sumar(LocalDate desde, LocalDate hasta) {
        BigDecimal total = pagoRepository.sumarPagados(desde.atStartOfDay(), hasta.atStartOfDay());
        return total == null ? BigDecimal.ZERO : total;
    }
}
