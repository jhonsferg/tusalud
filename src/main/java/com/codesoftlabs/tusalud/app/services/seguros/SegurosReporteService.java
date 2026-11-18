package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.dto.response.ReporteFinancieroResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CarteraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.SegurosResumenResponse;
import com.codesoftlabs.tusalud.app.entities.CuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoLiquidacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.CuentaAseguradoraRepository;
import com.codesoftlabs.tusalud.app.repositories.LiquidacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PolizaRepository;
import com.codesoftlabs.tusalud.app.repositories.PreautorizacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegurosReporteService {

    private static final int DIAS_POR_VENCER = 30;

    private final CuentaAseguradoraRepository cuentaRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final PolizaRepository polizaRepository;
    private final PreautorizacionRepository preautorizacionRepository;
    private final LiquidacionService liquidacionService;

    public ReporteFinancieroResponse.Seguros seguros(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        BigDecimal cubierto = BigDecimal.ZERO;
        BigDecimal copago = BigDecimal.ZERO;
        Map<String, long[]> cuentasPor = new LinkedHashMap<>();
        Map<String, BigDecimal[]> montosPor = new LinkedHashMap<>();
        List<CuentaAseguradora> periodo = cuentaRepository.vigentesEnRango(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        for (CuentaAseguradora cuenta : periodo) {
            cubierto = cubierto.add(cuenta.getMontoCubierto());
            copago = copago.add(cuenta.getCopago());
            String nombre = cuenta.getAseguradora().getNombre();
            cuentasPor.computeIfAbsent(nombre, k -> new long[1])[0]++;
            BigDecimal[] montos = montosPor.computeIfAbsent(
                    nombre, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            montos[0] = montos[0].add(cuenta.getMontoCubierto());
            montos[1] = montos[1].add(cuenta.getCopago());
            if (cuenta.getEstado() == EstadoCuentaAseguradora.PENDIENTE
                    || cuenta.getEstado() == EstadoCuentaAseguradora.LIQUIDADA) {
                montos[2] = montos[2].add(cuenta.getMontoCubierto());
            }
        }
        List<ReporteFinancieroResponse.PorAseguradora> porAseguradora = montosPor.entrySet().stream()
                .map(e -> new ReporteFinancieroResponse.PorAseguradora(
                        e.getKey(), cuentasPor.get(e.getKey())[0], e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
        List<CarteraResponse> cartera = liquidacionService.cartera();
        BigDecimal pendiente = BigDecimal.ZERO;
        BigDecimal[] tramos = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        long[] cantidades = new long[4];
        for (CuentaAseguradora cuenta : cuentaRepository.porEstado(EstadoCuentaAseguradora.PENDIENTE)) {
            long dias = ChronoUnit.DAYS.between(cuenta.getFecha().toLocalDate(), hoy);
            int indice = dias <= 30 ? 0 : dias <= 60 ? 1 : dias <= 90 ? 2 : 3;
            tramos[indice] = tramos[indice].add(cuenta.getMontoCubierto());
            cantidades[indice]++;
            pendiente = pendiente.add(cuenta.getMontoCubierto());
        }
        List<ReporteFinancieroResponse.Tramo> antiguedad = List.of(
                new ReporteFinancieroResponse.Tramo("0 a 30 días", cantidades[0], tramos[0]),
                new ReporteFinancieroResponse.Tramo("31 a 60 días", cantidades[1], tramos[1]),
                new ReporteFinancieroResponse.Tramo("61 a 90 días", cantidades[2], tramos[2]),
                new ReporteFinancieroResponse.Tramo("Más de 90 días", cantidades[3], tramos[3]));
        return new ReporteFinancieroResponse.Seguros(
                cubierto.setScale(2, java.math.RoundingMode.HALF_UP),
                copago.setScale(2, java.math.RoundingMode.HALF_UP),
                pendiente.setScale(2, java.math.RoundingMode.HALF_UP),
                porAseguradora,
                antiguedad);
    }

    public SegurosResumenResponse resumen(UsuarioPrincipal actor) {
        boolean cartera = tiene(actor, RolNombre.ROLE_FINANZAS)
                || tiene(actor, RolNombre.ROLE_GERENCIA)
                || tiene(actor, RolNombre.ROLE_ADMIN);
        boolean recepcion = tiene(actor, RolNombre.ROLE_RECEPCION) || tiene(actor, RolNombre.ROLE_ADMIN);
        BigDecimal total = null;
        Long cuentas = null;
        Long liquidaciones = null;
        if (cartera) {
            List<CarteraResponse> detalle = liquidacionService.cartera();
            total = detalle.stream().map(CarteraResponse::pendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
            cuentas = detalle.stream().mapToLong(CarteraResponse::cuentas).sum();
            liquidaciones = liquidacionRepository.countByEstadoIn(
                    EnumSet.of(EstadoLiquidacion.BORRADOR, EstadoLiquidacion.ENVIADA, EstadoLiquidacion.OBSERVADA));
        }
        Long porVencer = null;
        if (recepcion || cartera) {
            LocalDate hoy = LocalDate.now();
            porVencer = polizaRepository.contarPorVencer(hoy, hoy.plusDays(DIAS_POR_VENCER));
        }
        Long pendientes =
                recepcion || cartera ? preautorizacionRepository.countByEstado(EstadoPreautorizacion.SOLICITADA) : null;
        return new SegurosResumenResponse(total, cuentas, liquidaciones, porVencer, pendientes);
    }

    private static boolean tiene(UsuarioPrincipal actor, RolNombre rol) {
        return actor.getRoles().contains(rol.name());
    }
}
