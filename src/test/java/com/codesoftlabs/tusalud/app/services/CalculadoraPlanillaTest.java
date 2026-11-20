package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.RegimenLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.SistemaPensionario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoConcepto;
import com.codesoftlabs.tusalud.app.services.planilla.CalculadoraPlanilla;
import com.codesoftlabs.tusalud.app.services.planilla.CalculadoraPlanilla.ConceptoAplicado;
import com.codesoftlabs.tusalud.app.services.planilla.CalculadoraPlanilla.Entrada;
import com.codesoftlabs.tusalud.app.services.planilla.CalculadoraPlanilla.Linea;
import com.codesoftlabs.tusalud.app.services.planilla.CalculadoraPlanilla.Parametros;
import com.codesoftlabs.tusalud.app.services.planilla.CalculadoraPlanilla.Resultado;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculadoraPlanillaTest {

    static final Map<String, BigDecimal> VALORES = Map.ofEntries(
            Map.entry("RMV", new BigDecimal("1130")),
            Map.entry("ASIGNACION_FAMILIAR_PORC", new BigDecimal("10")),
            Map.entry("ESSALUD_TASA", new BigDecimal("9")),
            Map.entry("ONP_TASA", new BigDecimal("13")),
            Map.entry("AFP_APORTE_TASA", new BigDecimal("10")),
            Map.entry("AFP_SEGURO_TASA", new BigDecimal("1.37")),
            Map.entry("AFP_COMISION_TASA", new BigDecimal("1.55")),
            Map.entry("UIT", new BigDecimal("5350")),
            Map.entry("RENTA5_DEDUCCION_UIT", new BigDecimal("7")),
            Map.entry("RENTA5_LIMITE_1_UIT", new BigDecimal("5")),
            Map.entry("RENTA5_LIMITE_2_UIT", new BigDecimal("20")),
            Map.entry("RENTA5_LIMITE_3_UIT", new BigDecimal("35")),
            Map.entry("RENTA5_LIMITE_4_UIT", new BigDecimal("45")),
            Map.entry("RENTA5_TASA_1", new BigDecimal("8")),
            Map.entry("RENTA5_TASA_2", new BigDecimal("14")),
            Map.entry("RENTA5_TASA_3", new BigDecimal("17")),
            Map.entry("RENTA5_TASA_4", new BigDecimal("20")),
            Map.entry("RENTA5_TASA_5", new BigDecimal("30")),
            Map.entry("HORAS_EXTRA_SOBRETASA_1", new BigDecimal("25")),
            Map.entry("HORAS_EXTRA_SOBRETASA_2", new BigDecimal("35")),
            Map.entry("HORAS_EXTRA_TRAMO_MINUTOS", new BigDecimal("120")),
            Map.entry("TARDANZA_TOLERANCIA_MINUTOS", new BigDecimal("10")),
            Map.entry("DIAS_MES", new BigDecimal("30")),
            Map.entry("HORAS_JORNADA", new BigDecimal("8")));

    private static final Parametros PARAMETROS = new Parametros(VALORES);

    private static Entrada entrada(
            RegimenLaboral regimen,
            SistemaPensionario pension,
            String remuneracion,
            boolean asignacion,
            int faltas,
            int sinGoce,
            int tardanza,
            int extra1,
            int extra2,
            List<ConceptoAplicado> conceptos) {
        return new Entrada(
                regimen,
                pension,
                new BigDecimal(remuneracion),
                asignacion,
                30,
                faltas,
                sinGoce,
                tardanza,
                extra1,
                extra2,
                conceptos);
    }

    private static BigDecimal monto(Resultado r, String codigo) {
        return r.lineas().stream()
                .filter(l -> l.codigo().equals(codigo))
                .map(Linea::monto)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Test
    void sueldoDeTresMilConOnpCalculaPensionRentaYEssalud() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "3000", false, 0, 0, 0, 0, 0, List.of()),
                PARAMETROS);

        assertThat(r.totalIngresos()).isEqualByComparingTo("3000.00");
        assertThat(r.pension()).isEqualByComparingTo("390.00");
        assertThat(r.rentaQuinta()).isEqualByComparingTo("30.33");
        assertThat(r.aporteEmpleador()).isEqualByComparingTo("270.00");
        assertThat(r.totalDescuentos()).isEqualByComparingTo("420.33");
        assertThat(r.neto()).isEqualByComparingTo("2579.67");
    }

    @Test
    void conAfpSumaAporteSeguroYComision() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.AFP, "2000", false, 0, 0, 0, 0, 0, List.of()),
                PARAMETROS);

        assertThat(r.pension()).isEqualByComparingTo("258.40");
        assertThat(r.rentaQuinta()).isEqualByComparingTo("0.00");
        assertThat(r.neto()).isEqualByComparingTo("1741.60");
    }

    @Test
    void laAsignacionFamiliarEsUnDecimoDeLaRemuneracionMinima() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "2000", true, 0, 0, 0, 0, 0, List.of()),
                PARAMETROS);

        assertThat(monto(r, "ASIG_FAM")).isEqualByComparingTo("113.00");
        assertThat(r.totalIngresos()).isEqualByComparingTo("2113.00");
        assertThat(r.pension()).isEqualByComparingTo("274.69");
    }

    @Test
    void faltasYTardanzasDescuentanDelBasicoYBajanLaBaseDeAportes() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "3000", false, 2, 1, 90, 0, 0, List.of()),
                PARAMETROS);

        assertThat(monto(r, "DESC_FALTAS")).isEqualByComparingTo("300.00");
        assertThat(monto(r, "DESC_TARDANZA")).isEqualByComparingTo("18.75");
        assertThat(monto(r, "PENSION")).isEqualByComparingTo("348.56");
        assertThat(monto(r, "RENTA_5TA")).isEqualByComparingTo("0.58");
        assertThat(r.neto()).isEqualByComparingTo("2332.11");
    }

    @Test
    void lasHorasExtraSeValorizanConSobretasaPorTramo() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "2400", false, 0, 0, 0, 120, 60, List.of()),
                PARAMETROS);

        assertThat(monto(r, "HORAS_EXTRA")).isEqualByComparingTo("38.50");
        assertThat(r.totalIngresos()).isEqualByComparingTo("2438.50");
    }

    @Test
    void losConceptosConfigurablesSumanYRestan() {
        List<ConceptoAplicado> conceptos = List.of(
                new ConceptoAplicado(
                        "BONO_PRODUCTIVIDAD", "Bono", TipoConcepto.INGRESO, new BigDecimal("150.00"), true, true, true),
                new ConceptoAplicado(
                        "MOVILIDAD", "Movilidad", TipoConcepto.INGRESO, new BigDecimal("100.00"), false, false, false),
                new ConceptoAplicado(
                        "PRESTAMO", "Préstamo", TipoConcepto.DESCUENTO, new BigDecimal("200.00"), false, false, false));
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "2000", false, 0, 0, 0, 0, 0, conceptos),
                PARAMETROS);

        assertThat(r.totalIngresos()).isEqualByComparingTo("2250.00");
        assertThat(monto(r, "PENSION")).isEqualByComparingTo("279.50");
        assertThat(monto(r, "PRESTAMO")).isEqualByComparingTo("200.00");
        assertThat(r.neto()).isEqualByComparingTo("1770.50");
    }

    @Test
    void unDescuentoRecurrenteNuncaDejaElNetoNegativo() {
        List<ConceptoAplicado> conceptos = List.of(new ConceptoAplicado(
                "ADELANTO", "Adelanto", TipoConcepto.DESCUENTO, new BigDecimal("5000.00"), false, false, false));
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "1500", false, 0, 0, 0, 0, 0, conceptos),
                PARAMETROS);

        assertThat(r.neto()).isEqualByComparingTo("0.00");
        assertThat(monto(r, "ADELANTO")).isEqualByComparingTo("1305.00");
    }

    @Test
    void losPracticantesNoAportanPensionNiRenta() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.PRACTICAS, SistemaPensionario.ONP, "1200", true, 0, 0, 0, 0, 0, List.of()),
                PARAMETROS);

        assertThat(r.pension()).isEqualByComparingTo("0.00");
        assertThat(r.rentaQuinta()).isEqualByComparingTo("0.00");
        assertThat(monto(r, "ASIG_FAM")).isEqualByComparingTo("0.00");
        assertThat(r.neto()).isEqualByComparingTo("1200.00");
        assertThat(r.provisionCts()).isEqualByComparingTo("0.00");
    }

    @Test
    void essaludTomaComoMinimoLaRemuneracionMinima() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.MICROEMPRESA, SistemaPensionario.ONP, "900", false, 0, 0, 0, 0, 0, List.of()),
                PARAMETROS);

        assertThat(r.aporteEmpleador()).isEqualByComparingTo("101.70");
        assertThat(r.provisionGratificacion()).isEqualByComparingTo("0.00");
    }

    @Test
    void lasProvisionesDelRegimenGeneralSonUnSextoYUnDoceavo() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "3000", false, 0, 0, 0, 0, 0, List.of()),
                PARAMETROS);

        assertThat(r.provisionGratificacion()).isEqualByComparingTo("500.00");
        assertThat(r.provisionCts()).isEqualByComparingTo("256.94");
        assertThat(r.provisionVacaciones()).isEqualByComparingTo("250.00");
    }

    @Test
    void laRentaCrecePorTramosConIngresosAltos() {
        Resultado r = CalculadoraPlanilla.calcular(
                entrada(RegimenLaboral.GENERAL, SistemaPensionario.ONP, "14000", false, 0, 0, 0, 0, 0, List.of()),
                PARAMETROS);

        assertThat(r.rentaQuinta()).isEqualByComparingTo("1844.88");
    }

    @Test
    void elProrrateoPorDiasPagablesReduceElBasico() {
        Entrada parcial = new Entrada(
                RegimenLaboral.GENERAL,
                SistemaPensionario.ONP,
                new BigDecimal("3000"),
                false,
                15,
                0,
                0,
                0,
                0,
                0,
                List.of());
        Resultado r = CalculadoraPlanilla.calcular(parcial, PARAMETROS);

        assertThat(monto(r, "BASICO")).isEqualByComparingTo("1500.00");
    }
}
