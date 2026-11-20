package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ResumenContabilidad;
import com.codesoftlabs.tusalud.app.entities.PeriodoContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoLineaExtracto;
import com.codesoftlabs.tusalud.app.repositories.AsientoRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaTesoreriaRepository;
import com.codesoftlabs.tusalud.app.repositories.ExtractoLineaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumenContabilidadService {

    private final PeriodoContableService periodos;
    private final AsientoRepository asientos;
    private final AsientosPendientesService pendientes;
    private final TesoreriaService tesoreria;
    private final CuentaTesoreriaRepository cuentasTesoreria;
    private final ExtractoLineaRepository lineas;
    private final FlujoCajaService flujo;

    public ResumenContabilidad resumen() {
        PeriodoContable periodo = periodos.actual().orElse(null);
        long extractos = cuentasTesoreria.activas().stream()
                .mapToLong(c -> lineas.countByCuentaIdAndEstado(c.getId(), EstadoLineaExtracto.PENDIENTE))
                .sum();
        return new ResumenContabilidad(
                periodo == null ? null : periodo.etiqueta(),
                periodo == null ? null : periodo.getEstado(),
                periodo == null ? 0 : asientos.countByPeriodoIdAndEstadoNot(periodo.getId(), EstadoAsiento.BORRADOR),
                periodo == null ? 0 : asientos.contarBorradores(periodo.getId()),
                pendientes.abiertos(),
                tesoreria.saldoTotal(),
                extractos,
                flujo.totalPorPagar(),
                flujo.totalPorCobrar());
    }
}
