package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.BalanceComprobacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.EstadoResultadosResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.EstadoSituacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.FilaBalance;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.FilaDiario;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.FilaMayor;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.LibroMayorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.LineaEstado;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.SeccionEstado;
import com.codesoftlabs.tusalud.app.entities.Asiento;
import com.codesoftlabs.tusalud.app.entities.AsientoLinea;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.NaturalezaCuenta;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaContable;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AsientoRepository;
import com.codesoftlabs.tusalud.app.repositories.AsientoRepository.SaldoCuenta;
import com.codesoftlabs.tusalud.app.repositories.CuentaContableRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibrosContablesService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final AsientoRepository asientos;
    private final CuentaContableRepository cuentas;

    public List<FilaDiario> diario(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<FilaDiario> filas = new ArrayList<>();
        for (Asiento asiento : asientos.contabilizadosEntre(desde, hasta)) {
            for (AsientoLinea linea : asiento.getLineas()) {
                filas.add(new FilaDiario(
                        asiento.getFecha(),
                        asiento.getNumero(),
                        asiento.getOrigen(),
                        asiento.getEstado(),
                        linea.getCuenta().getCodigo(),
                        linea.getCuenta().getNombre(),
                        linea.getGlosa() == null ? asiento.getGlosa() : linea.getGlosa(),
                        linea.getDebe(),
                        linea.getHaber()));
            }
        }
        return filas;
    }

    public LibroMayorResponse mayor(UUID cuentaId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        CuentaContable cuenta =
                cuentas.findById(cuentaId).orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", cuentaId));
        boolean deudora = cuenta.getNaturaleza() == NaturalezaCuenta.DEUDORA;
        BigDecimal inicial = asientos.saldosAntesDe(desde).stream()
                .filter(s -> s.getCuentaId().equals(cuentaId))
                .findFirst()
                .map(s -> neto(s, deudora))
                .orElse(CERO);
        BigDecimal saldo = inicial;
        BigDecimal totalDebe = CERO;
        BigDecimal totalHaber = CERO;
        List<FilaMayor> filas = new ArrayList<>();
        for (AsientoLinea linea : asientos.movimientosDeCuenta(cuentaId, desde, hasta)) {
            BigDecimal variacion = deudora
                    ? linea.getDebe().subtract(linea.getHaber())
                    : linea.getHaber().subtract(linea.getDebe());
            saldo = saldo.add(variacion);
            totalDebe = totalDebe.add(linea.getDebe());
            totalHaber = totalHaber.add(linea.getHaber());
            filas.add(new FilaMayor(
                    linea.getAsiento().getFecha(),
                    linea.getAsiento().getNumero(),
                    linea.getAsiento().getId(),
                    linea.getGlosa() == null ? linea.getAsiento().getGlosa() : linea.getGlosa(),
                    linea.getDebe(),
                    linea.getHaber(),
                    saldo));
        }
        return new LibroMayorResponse(
                ContabilidadMapper.cuenta(cuenta), desde, hasta, inicial, totalDebe, totalHaber, saldo, filas);
    }

    public BalanceComprobacionResponse balanceComprobacion(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        Map<UUID, SaldoCuenta> iniciales = indexar(asientos.saldosAntesDe(desde));
        Map<UUID, SaldoCuenta> periodo = indexar(asientos.saldosEntre(desde, hasta));
        List<CuentaContable> plan = cuentas.findAllByOrderByCodigoAsc();
        List<FilaBalance> hojas = new ArrayList<>();
        for (CuentaContable cuenta : plan) {
            BigDecimal inicialNeto = netoDebe(iniciales.get(cuenta.getId()));
            BigDecimal debe = valorDebe(periodo.get(cuenta.getId()));
            BigDecimal haber = valorHaber(periodo.get(cuenta.getId()));
            if (inicialNeto.signum() == 0 && debe.signum() == 0 && haber.signum() == 0) {
                continue;
            }
            hojas.add(fila(cuenta, false, inicialNeto, debe, haber));
        }
        List<FilaBalance> filas = new ArrayList<>(hojas);
        for (CuentaContable cuenta : plan) {
            if (cuenta.isAceptaMovimientos()) {
                continue;
            }
            List<FilaBalance> hijas = hojas.stream()
                    .filter(h -> h.codigo().startsWith(cuenta.getCodigo())
                            && !h.codigo().equals(cuenta.getCodigo()))
                    .toList();
            if (hijas.isEmpty()) {
                continue;
            }
            BigDecimal inicialNeto = hijas.stream()
                    .map(h -> h.inicialDeudor().subtract(h.inicialAcreedor()))
                    .reduce(CERO, BigDecimal::add);
            BigDecimal debe = hijas.stream().map(FilaBalance::debe).reduce(CERO, BigDecimal::add);
            BigDecimal haber = hijas.stream().map(FilaBalance::haber).reduce(CERO, BigDecimal::add);
            filas.add(fila(cuenta, true, inicialNeto, debe, haber));
        }
        filas.sort(Comparator.comparing(FilaBalance::codigo));
        BigDecimal totalDebe = hojas.stream().map(FilaBalance::debe).reduce(CERO, BigDecimal::add);
        BigDecimal totalHaber = hojas.stream().map(FilaBalance::haber).reduce(CERO, BigDecimal::add);
        BigDecimal totalDeudor = hojas.stream().map(FilaBalance::finalDeudor).reduce(CERO, BigDecimal::add);
        BigDecimal totalAcreedor =
                hojas.stream().map(FilaBalance::finalAcreedor).reduce(CERO, BigDecimal::add);
        boolean cuadra = totalDebe.compareTo(totalHaber) == 0 && totalDeudor.compareTo(totalAcreedor) == 0;
        return new BalanceComprobacionResponse(
                desde, hasta, filas, totalDebe, totalHaber, totalDeudor, totalAcreedor, cuadra);
    }

    public EstadoSituacionResponse estadoSituacion(LocalDate al) {
        LocalDate inicioAnio = LocalDate.of(al.getYear(), 1, 1);
        Map<UUID, SaldoCuenta> acumulado = indexar(asientos.saldosAntesDe(al.plusDays(1)));
        Map<UUID, SaldoCuenta> anteriores = indexar(asientos.saldosAntesDe(inicioAnio));
        List<CuentaContable> plan = cuentas.findAllByOrderByCodigoAsc();
        List<LineaEstado> activos = new ArrayList<>();
        List<LineaEstado> pasivos = new ArrayList<>();
        List<LineaEstado> patrimonio = new ArrayList<>();
        BigDecimal resultadoTotal = CERO;
        BigDecimal resultadoAnteriores = CERO;
        for (CuentaContable cuenta : plan) {
            SaldoCuenta saldo = acumulado.get(cuenta.getId());
            if (saldo == null) {
                continue;
            }
            switch (cuenta.getTipo()) {
                case ACTIVO -> agregar(activos, cuenta, valorDebe(saldo).subtract(valorHaber(saldo)));
                case PASIVO -> agregar(pasivos, cuenta, valorHaber(saldo).subtract(valorDebe(saldo)));
                case PATRIMONIO -> agregar(patrimonio, cuenta, valorHaber(saldo).subtract(valorDebe(saldo)));
                case INGRESO ->
                    resultadoTotal = resultadoTotal.add(valorHaber(saldo).subtract(valorDebe(saldo)));
                case GASTO ->
                    resultadoTotal = resultadoTotal.subtract(valorDebe(saldo).subtract(valorHaber(saldo)));
            }
            SaldoCuenta previo = anteriores.get(cuenta.getId());
            if (previo != null && cuenta.getTipo() == TipoCuentaContable.INGRESO) {
                resultadoAnteriores = resultadoAnteriores.add(valorHaber(previo).subtract(valorDebe(previo)));
            }
            if (previo != null && cuenta.getTipo() == TipoCuentaContable.GASTO) {
                resultadoAnteriores =
                        resultadoAnteriores.subtract(valorDebe(previo).subtract(valorHaber(previo)));
            }
        }
        BigDecimal resultadoEjercicio = resultadoTotal.subtract(resultadoAnteriores);
        if (resultadoAnteriores.signum() != 0) {
            patrimonio.add(new LineaEstado("", "Resultados de ejercicios anteriores", resultadoAnteriores));
        }
        if (resultadoEjercicio.signum() != 0) {
            patrimonio.add(new LineaEstado("", "Resultado del ejercicio", resultadoEjercicio));
        }
        SeccionEstado activo = seccion("Activo", activos);
        SeccionEstado pasivo = seccion("Pasivo", pasivos);
        SeccionEstado patrimonioNeto = seccion("Patrimonio", patrimonio);
        BigDecimal pasivoPatrimonio = pasivo.total().add(patrimonioNeto.total());
        return new EstadoSituacionResponse(
                al,
                activo,
                pasivo,
                patrimonioNeto,
                pasivoPatrimonio,
                activo.total().compareTo(pasivoPatrimonio) == 0);
    }

    public EstadoResultadosResponse estadoResultados(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        Map<UUID, SaldoCuenta> periodo = indexar(asientos.saldosEntre(desde, hasta));
        List<LineaEstado> ingresos = new ArrayList<>();
        List<LineaEstado> gastos = new ArrayList<>();
        for (CuentaContable cuenta : cuentas.findAllByOrderByCodigoAsc()) {
            SaldoCuenta saldo = periodo.get(cuenta.getId());
            if (saldo == null) {
                continue;
            }
            if (cuenta.getTipo() == TipoCuentaContable.INGRESO) {
                agregar(ingresos, cuenta, valorHaber(saldo).subtract(valorDebe(saldo)));
            } else if (cuenta.getTipo() == TipoCuentaContable.GASTO) {
                agregar(gastos, cuenta, valorDebe(saldo).subtract(valorHaber(saldo)));
            }
        }
        SeccionEstado ing = seccion("Ingresos", ingresos);
        SeccionEstado gas = seccion("Gastos", gastos);
        return new EstadoResultadosResponse(desde, hasta, ing, gas, ing.total().subtract(gas.total()));
    }

    private FilaBalance fila(
            CuentaContable cuenta, boolean agregada, BigDecimal inicialNeto, BigDecimal debe, BigDecimal haber) {
        BigDecimal finalNeto = inicialNeto.add(debe).subtract(haber);
        return new FilaBalance(
                cuenta.getCodigo(),
                cuenta.getNombre(),
                cuenta.getTipo(),
                agregada,
                inicialNeto.signum() > 0 ? inicialNeto : CERO,
                inicialNeto.signum() < 0 ? inicialNeto.negate() : CERO,
                debe,
                haber,
                finalNeto.signum() > 0 ? finalNeto : CERO,
                finalNeto.signum() < 0 ? finalNeto.negate() : CERO);
    }

    private static void agregar(List<LineaEstado> lista, CuentaContable cuenta, BigDecimal importe) {
        if (importe.signum() != 0) {
            lista.add(new LineaEstado(cuenta.getCodigo(), cuenta.getNombre(), importe));
        }
    }

    private static SeccionEstado seccion(String titulo, List<LineaEstado> lineas) {
        BigDecimal total = lineas.stream().map(LineaEstado::importe).reduce(CERO, BigDecimal::add);
        return new SeccionEstado(titulo, lineas, total);
    }

    private static Map<UUID, SaldoCuenta> indexar(List<SaldoCuenta> saldos) {
        Map<UUID, SaldoCuenta> mapa = new HashMap<>();
        for (SaldoCuenta saldo : saldos) {
            mapa.put(saldo.getCuentaId(), saldo);
        }
        return mapa;
    }

    private static BigDecimal valorDebe(SaldoCuenta saldo) {
        return saldo == null ? CERO : saldo.getDebe().setScale(2);
    }

    private static BigDecimal valorHaber(SaldoCuenta saldo) {
        return saldo == null ? CERO : saldo.getHaber().setScale(2);
    }

    private static BigDecimal netoDebe(SaldoCuenta saldo) {
        return valorDebe(saldo).subtract(valorHaber(saldo));
    }

    private static BigDecimal neto(SaldoCuenta saldo, boolean deudora) {
        return deudora
                ? valorDebe(saldo).subtract(valorHaber(saldo))
                : valorHaber(saldo).subtract(valorDebe(saldo));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new DatosInvalidosException("El rango de fechas no es válido");
        }
    }
}
