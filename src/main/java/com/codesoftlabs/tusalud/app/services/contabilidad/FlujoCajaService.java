package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.FlujoCajaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.SemanaFlujo;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.services.contabilidad.OrigenesContables.VencimientoPorCobrar;
import com.codesoftlabs.tusalud.app.services.contabilidad.OrigenesContables.VencimientoPorPagar;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlujoCajaService {

    private static final int MAXIMO_SEMANAS = 26;

    private final OrigenesContables origenes;
    private final TesoreriaService tesoreria;

    @Value("${app.contabilidad.plazo-cobro-aseguradora-dias:30}")
    private int plazoCobroAseguradora;

    public FlujoCajaResponse proyectar(int semanas) {
        if (semanas < 1 || semanas > MAXIMO_SEMANAS) {
            throw new DatosInvalidosException("Las semanas deben estar entre 1 y " + MAXIMO_SEMANAS);
        }
        LocalDate hoy = LocalDate.now();
        BigDecimal inicial = tesoreria.saldoTotal();
        List<VencimientoPorCobrar> cobros = origenes.porCobrar(plazoCobroAseguradora);
        List<VencimientoPorPagar> pagos = origenes.porPagar();
        List<SemanaFlujo> detalle = new ArrayList<>();
        BigDecimal saldo = inicial;
        for (int i = 0; i < semanas; i++) {
            LocalDate desde = hoy.plusWeeks(i);
            LocalDate hasta = desde.plusDays(6);
            boolean primera = i == 0;
            BigDecimal entradas = cobros.stream()
                    .filter(c -> (primera || !c.fecha().isBefore(desde))
                            && !c.fecha().isAfter(hasta))
                    .map(VencimientoPorCobrar::monto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal salidas = pagos.stream()
                    .filter(p -> (primera || !p.fecha().isBefore(desde))
                            && !p.fecha().isAfter(hasta))
                    .map(VencimientoPorPagar::saldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal neto = entradas.subtract(salidas);
            saldo = saldo.add(neto);
            detalle.add(new SemanaFlujo(desde, hasta, entradas, salidas, neto, saldo));
        }
        return new FlujoCajaResponse(hoy, semanas, inicial, detalle, saldo);
    }

    public BigDecimal totalPorCobrar() {
        return origenes.porCobrar(plazoCobroAseguradora).stream()
                .map(VencimientoPorCobrar::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPorPagar() {
        return origenes.porPagar().stream().map(VencimientoPorPagar::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
