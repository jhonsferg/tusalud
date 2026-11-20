package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.RegimenLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.SistemaPensionario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoConcepto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CalculadoraPlanilla {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal DOCE = BigDecimal.valueOf(12);
    private static final BigDecimal SEIS = BigDecimal.valueOf(6);
    private static final BigDecimal SESENTA = BigDecimal.valueOf(60);
    private static final BigDecimal CERO = BigDecimal.ZERO;

    private CalculadoraPlanilla() {}

    public record Parametros(Map<String, BigDecimal> valores) {

        public BigDecimal get(String codigo) {
            BigDecimal valor = valores.get(codigo);
            if (valor == null) {
                throw new IllegalStateException("Falta el parámetro de planilla " + codigo);
            }
            return valor;
        }
    }

    public record ConceptoAplicado(
            String codigo,
            String nombre,
            TipoConcepto tipo,
            BigDecimal monto,
            boolean afectoRenta,
            boolean afectoPension,
            boolean afectoEssalud) {}

    public record Entrada(
            RegimenLaboral regimen,
            SistemaPensionario pension,
            BigDecimal remuneracion,
            boolean asignacionFamiliar,
            int diasPagables,
            int diasFalta,
            int diasNoRemunerados,
            int minutosTardanza,
            int minutosExtraTramo1,
            int minutosExtraTramo2,
            List<ConceptoAplicado> conceptos) {}

    public record Linea(String codigo, String nombre, TipoConcepto tipo, BigDecimal base, BigDecimal monto) {}

    public record Resultado(
            List<Linea> lineas,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal pension,
            BigDecimal rentaQuinta,
            BigDecimal aporteEmpleador,
            BigDecimal neto,
            BigDecimal provisionGratificacion,
            BigDecimal provisionCts,
            BigDecimal provisionVacaciones) {}

    public static Resultado calcular(Entrada e, Parametros p) {
        BigDecimal diasMes = p.get("DIAS_MES");
        BigDecimal horasJornada = p.get("HORAS_JORNADA");
        BigDecimal valorDia = e.remuneracion().divide(diasMes, 10, RoundingMode.HALF_UP);
        BigDecimal valorHora = valorDia.divide(horasJornada, 10, RoundingMode.HALF_UP);

        List<Linea> lineas = new ArrayList<>();

        BigDecimal basico = redondear(valorDia.multiply(BigDecimal.valueOf(e.diasPagables())));
        lineas.add(new Linea("BASICO", "Remuneración básica", TipoConcepto.INGRESO, e.remuneracion(), basico));

        BigDecimal asignacion = CERO;
        if (e.asignacionFamiliar() && e.regimen() != RegimenLaboral.PRACTICAS) {
            asignacion = redondear(
                    p.get("RMV").multiply(p.get("ASIGNACION_FAMILIAR_PORC")).divide(CIEN, 10, RoundingMode.HALF_UP));
            lineas.add(new Linea("ASIG_FAM", "Asignación familiar", TipoConcepto.INGRESO, p.get("RMV"), asignacion));
        }

        BigDecimal extras = CERO;
        if (e.minutosExtraTramo1() > 0 || e.minutosExtraTramo2() > 0) {
            BigDecimal factor1 =
                    BigDecimal.ONE.add(p.get("HORAS_EXTRA_SOBRETASA_1").divide(CIEN, 10, RoundingMode.HALF_UP));
            BigDecimal factor2 =
                    BigDecimal.ONE.add(p.get("HORAS_EXTRA_SOBRETASA_2").divide(CIEN, 10, RoundingMode.HALF_UP));
            BigDecimal horas1 = BigDecimal.valueOf(e.minutosExtraTramo1()).divide(SESENTA, 10, RoundingMode.HALF_UP);
            BigDecimal horas2 = BigDecimal.valueOf(e.minutosExtraTramo2()).divide(SESENTA, 10, RoundingMode.HALF_UP);
            extras = redondear(valorHora.multiply(horas1.multiply(factor1).add(horas2.multiply(factor2))));
            BigDecimal horasTotales = horas1.add(horas2).setScale(2, RoundingMode.HALF_UP);
            lineas.add(new Linea("HORAS_EXTRA", "Horas extra", TipoConcepto.INGRESO, horasTotales, extras));
        }

        BigDecimal otrosIngresos = CERO;
        BigDecimal otrosIngresosRenta = CERO;
        BigDecimal otrosIngresosPension = CERO;
        BigDecimal otrosIngresosEssalud = CERO;
        for (ConceptoAplicado c : e.conceptos()) {
            if (c.tipo() != TipoConcepto.INGRESO) {
                continue;
            }
            lineas.add(new Linea(c.codigo(), c.nombre(), TipoConcepto.INGRESO, null, c.monto()));
            otrosIngresos = otrosIngresos.add(c.monto());
            if (c.afectoRenta()) {
                otrosIngresosRenta = otrosIngresosRenta.add(c.monto());
            }
            if (c.afectoPension()) {
                otrosIngresosPension = otrosIngresosPension.add(c.monto());
            }
            if (c.afectoEssalud()) {
                otrosIngresosEssalud = otrosIngresosEssalud.add(c.monto());
            }
        }

        BigDecimal descuentoFaltas =
                redondear(valorDia.multiply(BigDecimal.valueOf((long) e.diasFalta() + e.diasNoRemunerados())));
        descuentoFaltas = descuentoFaltas.min(basico);
        BigDecimal descuentoTardanza = redondear(
                valorHora.multiply(BigDecimal.valueOf(e.minutosTardanza())).divide(SESENTA, 10, RoundingMode.HALF_UP));
        descuentoTardanza = descuentoTardanza.min(basico.subtract(descuentoFaltas));

        BigDecimal ingresosBrutos = basico.add(asignacion).add(extras).add(otrosIngresos);
        BigDecimal menoresIngresos = descuentoFaltas.add(descuentoTardanza);

        BigDecimal baseBase = basico.add(asignacion).add(extras).subtract(menoresIngresos);
        BigDecimal basePension = baseBase.add(otrosIngresosPension).max(CERO);
        BigDecimal baseRenta = baseBase.add(otrosIngresosRenta).max(CERO);
        BigDecimal baseEssalud = baseBase.add(otrosIngresosEssalud).max(p.get("RMV"));

        boolean practicas = e.regimen() == RegimenLaboral.PRACTICAS;

        BigDecimal pension = CERO;
        if (!practicas) {
            BigDecimal tasa = e.pension() == SistemaPensionario.ONP
                    ? p.get("ONP_TASA")
                    : p.get("AFP_APORTE_TASA").add(p.get("AFP_SEGURO_TASA")).add(p.get("AFP_COMISION_TASA"));
            pension = redondear(basePension.multiply(tasa).divide(CIEN, 10, RoundingMode.HALF_UP));
        }

        BigDecimal renta = practicas ? CERO : retencionRenta(baseRenta, e.regimen(), p);

        BigDecimal aporteEmpleador =
                redondear(baseEssalud.multiply(p.get("ESSALUD_TASA")).divide(CIEN, 10, RoundingMode.HALF_UP));

        if (descuentoFaltas.signum() > 0) {
            lineas.add(new Linea(
                    "DESC_FALTAS",
                    "Descuento por faltas y permisos sin goce",
                    TipoConcepto.DESCUENTO,
                    BigDecimal.valueOf((long) e.diasFalta() + e.diasNoRemunerados()),
                    descuentoFaltas));
        }
        if (descuentoTardanza.signum() > 0) {
            lineas.add(new Linea(
                    "DESC_TARDANZA",
                    "Descuento por tardanzas",
                    TipoConcepto.DESCUENTO,
                    BigDecimal.valueOf(e.minutosTardanza()),
                    descuentoTardanza));
        }
        if (pension.signum() > 0) {
            lineas.add(new Linea(
                    "PENSION",
                    e.pension() == SistemaPensionario.ONP ? "Aporte a la ONP" : "Aporte a la AFP",
                    TipoConcepto.DESCUENTO,
                    basePension,
                    pension));
        }
        if (renta.signum() > 0) {
            lineas.add(new Linea(
                    "RENTA_5TA", "Retención de renta de quinta categoría", TipoConcepto.DESCUENTO, baseRenta, renta));
        }

        BigDecimal descuentosDeLey = menoresIngresos.add(pension).add(renta);
        BigDecimal capacidad = ingresosBrutos.subtract(descuentosDeLey).max(CERO);
        BigDecimal otrosDescuentos = CERO;
        for (ConceptoAplicado c : e.conceptos()) {
            if (c.tipo() != TipoConcepto.DESCUENTO) {
                continue;
            }
            BigDecimal monto =
                    c.monto().min(capacidad.subtract(otrosDescuentos)).max(CERO);
            if (monto.signum() == 0) {
                continue;
            }
            lineas.add(new Linea(c.codigo(), c.nombre(), TipoConcepto.DESCUENTO, null, monto));
            otrosDescuentos = otrosDescuentos.add(monto);
        }

        lineas.add(
                new Linea("ESSALUD", "Aporte a EsSalud", TipoConcepto.APORTE_EMPLEADOR, baseEssalud, aporteEmpleador));

        BigDecimal totalDescuentos = descuentosDeLey.add(otrosDescuentos);
        BigDecimal neto = ingresosBrutos.subtract(totalDescuentos);

        BigDecimal factorGratificacion = factorGratificacion(e.regimen());
        BigDecimal gratificacion = redondear(
                baseBase.max(CERO).divide(SEIS, 10, RoundingMode.HALF_UP).multiply(factorGratificacion));
        BigDecimal cts = redondear(baseBase.max(CERO)
                .add(gratificacion.divide(SEIS, 10, RoundingMode.HALF_UP))
                .divide(DOCE, 10, RoundingMode.HALF_UP)
                .multiply(factorGratificacion));
        BigDecimal vacaciones = practicas
                ? CERO
                : redondear(baseBase.max(CERO)
                        .divide(DOCE, 10, RoundingMode.HALF_UP)
                        .multiply(factorVacaciones(e.regimen())));

        return new Resultado(
                List.copyOf(lineas),
                redondear(ingresosBrutos),
                redondear(totalDescuentos),
                pension,
                renta,
                aporteEmpleador,
                redondear(neto),
                gratificacion,
                cts,
                vacaciones);
    }

    static BigDecimal retencionRenta(BigDecimal baseMensual, RegimenLaboral regimen, Parametros p) {
        BigDecimal pagos = switch (regimen) {
            case GENERAL -> BigDecimal.valueOf(14);
            case PEQUENA_EMPRESA -> BigDecimal.valueOf(13);
            default -> BigDecimal.valueOf(12);
        };
        BigDecimal uit = p.get("UIT");
        BigDecimal anual = baseMensual.multiply(pagos);
        BigDecimal neta = anual.subtract(p.get("RENTA5_DEDUCCION_UIT").multiply(uit));
        if (neta.signum() <= 0) {
            return CERO;
        }
        BigDecimal[] limites = {
            p.get("RENTA5_LIMITE_1_UIT").multiply(uit),
            p.get("RENTA5_LIMITE_2_UIT").multiply(uit),
            p.get("RENTA5_LIMITE_3_UIT").multiply(uit),
            p.get("RENTA5_LIMITE_4_UIT").multiply(uit)
        };
        BigDecimal[] tasas = {
            p.get("RENTA5_TASA_1"),
            p.get("RENTA5_TASA_2"),
            p.get("RENTA5_TASA_3"),
            p.get("RENTA5_TASA_4"),
            p.get("RENTA5_TASA_5")
        };
        BigDecimal impuesto = CERO;
        BigDecimal anterior = CERO;
        for (int i = 0; i < limites.length; i++) {
            BigDecimal tramo = neta.min(limites[i]).subtract(anterior);
            if (tramo.signum() > 0) {
                impuesto = impuesto.add(tramo.multiply(tasas[i]).divide(CIEN, 10, RoundingMode.HALF_UP));
            }
            anterior = limites[i];
        }
        if (neta.compareTo(anterior) > 0) {
            impuesto = impuesto.add(neta.subtract(anterior).multiply(tasas[4]).divide(CIEN, 10, RoundingMode.HALF_UP));
        }
        return redondear(impuesto.divide(DOCE, 10, RoundingMode.HALF_UP));
    }

    private static BigDecimal factorGratificacion(RegimenLaboral regimen) {
        return switch (regimen) {
            case GENERAL -> BigDecimal.ONE;
            case PEQUENA_EMPRESA -> new BigDecimal("0.5");
            default -> CERO;
        };
    }

    private static BigDecimal factorVacaciones(RegimenLaboral regimen) {
        return regimen == RegimenLaboral.MICROEMPRESA || regimen == RegimenLaboral.PEQUENA_EMPRESA
                ? new BigDecimal("0.5")
                : BigDecimal.ONE;
    }

    static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
