package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.entities.Aseguradora;
import com.codesoftlabs.tusalud.app.entities.Convenio;
import com.codesoftlabs.tusalud.app.entities.ReglaCobertura;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCobertura;
import com.codesoftlabs.tusalud.app.services.seguros.CoberturaCalculadora;
import com.codesoftlabs.tusalud.app.services.seguros.CoberturaCalculadora.Linea;
import com.codesoftlabs.tusalud.app.services.seguros.CoberturaCalculadora.Resultado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoberturaCalculadoraTest {

    private final CoberturaCalculadora calculadora = new CoberturaCalculadora();

    private Convenio convenio(String topeAnual, ReglaCobertura... reglas) {
        Convenio convenio = Convenio.builder()
                .aseguradora(Aseguradora.builder().nombre("Seguro").activa(true).build())
                .nombre("Plan")
                .vigenciaDesde(LocalDate.now().minusDays(1))
                .vigenciaHasta(LocalDate.now().plusDays(100))
                .topeAnual(topeAnual == null ? null : new BigDecimal(topeAnual))
                .build();
        for (ReglaCobertura regla : reglas) {
            regla.setConvenio(convenio);
            convenio.getReglas().add(regla);
        }
        return convenio;
    }

    private ReglaCobertura regla(CategoriaServicio categoria, TipoCobertura tipo, String valor, String tope) {
        return ReglaCobertura.builder()
                .categoria(categoria)
                .tipo(tipo)
                .valor(new BigDecimal(valor))
                .topePorServicio(tope == null ? null : new BigDecimal(tope))
                .build();
    }

    private List<Linea> linea(String codigo, String monto) {
        return List.of(new Linea(codigo, "Servicio " + codigo, new BigDecimal(monto)));
    }

    @Test
    void elPorcentajeSeAplicaSobreElMonto() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.CONSULTA, TipoCobertura.PORCENTAJE, "80", null));
        Resultado r = calculadora.calcular(
                convenio, CategoriaServicio.CONSULTA, linea("CONSULTA", "100.00"), BigDecimal.ZERO);
        assertThat(r.cubierto()).isEqualByComparingTo("80.00");
        assertThat(r.copago()).isEqualByComparingTo("20.00");
        assertThat(r.total()).isEqualByComparingTo("100.00");
        assertThat(r.saldoTopeAnual()).isNull();
    }

    @Test
    void elCopagoFijoSeDescuentaDelMonto() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.CONSULTA, TipoCobertura.COPAGO_FIJO, "30", null));
        Resultado r = calculadora.calcular(
                convenio, CategoriaServicio.CONSULTA, linea("CONSULTA", "150.00"), BigDecimal.ZERO);
        assertThat(r.cubierto()).isEqualByComparingTo("120.00");
        assertThat(r.copago()).isEqualByComparingTo("30.00");
    }

    @Test
    void siElServicioCuestaMenosQueElCopagoFijoElPacientePagaTodo() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.CONSULTA, TipoCobertura.COPAGO_FIJO, "30", null));
        Resultado r =
                calculadora.calcular(convenio, CategoriaServicio.CONSULTA, linea("CONSULTA", "20.00"), BigDecimal.ZERO);
        assertThat(r.cubierto()).isEqualByComparingTo("0.00");
        assertThat(r.copago()).isEqualByComparingTo("20.00");
    }

    @Test
    void elRedondeoSeHaceACentimosYLaSumaCuadra() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.LABORATORIO, TipoCobertura.PORCENTAJE, "80", null));
        Resultado r =
                calculadora.calcular(convenio, CategoriaServicio.LABORATORIO, linea("HEM", "33.33"), BigDecimal.ZERO);
        assertThat(r.cubierto()).isEqualByComparingTo("26.66");
        assertThat(r.copago()).isEqualByComparingTo("6.67");
        assertThat(r.cubierto().add(r.copago())).isEqualByComparingTo(r.total());
    }

    @Test
    void elTopePorServicioLimitaLoCubierto() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.IMAGEN, TipoCobertura.PORCENTAJE, "80", "50.00"));
        Resultado r = calculadora.calcular(convenio, CategoriaServicio.IMAGEN, linea("RM", "100.00"), BigDecimal.ZERO);
        assertThat(r.cubierto()).isEqualByComparingTo("50.00");
        assertThat(r.copago()).isEqualByComparingTo("50.00");
        assertThat(r.lineas().get(0).motivo()).isEqualTo("Tope por servicio aplicado");
    }

    @Test
    void elTopeAnualAgotadoNoCubreNada() {
        Convenio convenio = convenio("1000", regla(CategoriaServicio.CONSULTA, TipoCobertura.PORCENTAJE, "80", null));
        Resultado r = calculadora.calcular(
                convenio, CategoriaServicio.CONSULTA, linea("CONSULTA", "100.00"), new BigDecimal("1000.00"));
        assertThat(r.cubierto()).isEqualByComparingTo("0.00");
        assertThat(r.copago()).isEqualByComparingTo("100.00");
        assertThat(r.saldoTopeAnual()).isEqualByComparingTo("0.00");
    }

    @Test
    void elTopeAnualParcialCubreSoloElSaldo() {
        Convenio convenio = convenio("1000", regla(CategoriaServicio.CONSULTA, TipoCobertura.PORCENTAJE, "80", null));
        Resultado r = calculadora.calcular(
                convenio, CategoriaServicio.CONSULTA, linea("CONSULTA", "100.00"), new BigDecimal("950.00"));
        assertThat(r.cubierto()).isEqualByComparingTo("50.00");
        assertThat(r.copago()).isEqualByComparingTo("50.00");
        assertThat(r.saldoTopeAnual()).isEqualByComparingTo("0.00");
        assertThat(r.lineas().get(0).motivo()).isEqualTo("Tope anual alcanzado");
    }

    @Test
    void elSaldoAnualSeConsumeEntreLasLineas() {
        Convenio convenio =
                convenio("100", regla(CategoriaServicio.LABORATORIO, TipoCobertura.PORCENTAJE, "100", null));
        Resultado r = calculadora.calcular(
                convenio,
                CategoriaServicio.LABORATORIO,
                List.of(new Linea("A", "A", new BigDecimal("60.00")), new Linea("B", "B", new BigDecimal("60.00"))),
                BigDecimal.ZERO);
        assertThat(r.lineas().get(0).cubierto()).isEqualByComparingTo("60.00");
        assertThat(r.lineas().get(1).cubierto()).isEqualByComparingTo("40.00");
        assertThat(r.cubierto()).isEqualByComparingTo("100.00");
        assertThat(r.copago()).isEqualByComparingTo("20.00");
        assertThat(r.saldoTopeAnual()).isEqualByComparingTo("0.00");
    }

    @Test
    void unaExclusionDejaElServicioSinCobertura() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.IMAGEN, TipoCobertura.PORCENTAJE, "70", null));
        convenio.getExclusiones().add("ECO-MAMA");
        Resultado r =
                calculadora.calcular(convenio, CategoriaServicio.IMAGEN, linea("eco-mama", "120.00"), BigDecimal.ZERO);
        assertThat(r.cubierto()).isEqualByComparingTo("0.00");
        assertThat(r.lineas().get(0).motivo()).isEqualTo("Servicio excluido del convenio");
    }

    @Test
    void unaCategoriaSinReglaNoSeCubre() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.CONSULTA, TipoCobertura.PORCENTAJE, "80", null));
        Resultado r =
                calculadora.calcular(convenio, CategoriaServicio.FARMACIA, linea("MED", "40.00"), BigDecimal.ZERO);
        assertThat(r.cubierto()).isEqualByComparingTo("0.00");
        assertThat(r.copago()).isEqualByComparingTo("40.00");
    }

    @Test
    void laCoberturaTotalDejaUnCopagoMinimoDeUnCentimo() {
        Convenio convenio = convenio(null, regla(CategoriaServicio.CONSULTA, TipoCobertura.PORCENTAJE, "100", null));
        Resultado r =
                calculadora.calcular(convenio, CategoriaServicio.CONSULTA, linea("CONSULTA", "10.00"), BigDecimal.ZERO);
        assertThat(r.copago()).isEqualByComparingTo("0.01");
        assertThat(r.cubierto()).isEqualByComparingTo("9.99");
    }
}
