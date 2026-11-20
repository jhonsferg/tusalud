package com.codesoftlabs.tusalud.app.services.pediatria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.IndicadorCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.NivelAlerta;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraCrecimiento.Evaluacion;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraCrecimiento.PuntoLms;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraCrecimiento.Resultado;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraCrecimientoTest {

    private static final List<PuntoLms> TABLA = List.of(
            new PuntoLms(0, 0.3487, 3.3464, 0.14602),
            new PuntoLms(12, -0.0506, 9.6479, 0.11316),
            new PuntoLms(24, -0.2024, 12.1515, 0.11967));

    @Test
    void laMedianaTieneZCeroYPercentilCincuenta() {
        Resultado r = CalculadoraCrecimiento.evaluar(TABLA.get(1), 9.6479);

        assertThat(r.z()).isCloseTo(0.0, within(1e-9));
        assertThat(r.percentil()).isCloseTo(50.0, within(1e-6));
    }

    @Test
    void unValorEnMenosDosDesviacionesDaPercentilDosPuntoTres() {
        PuntoLms lms = TABLA.get(1);
        double valor = CalculadoraCrecimiento.valorEnZ(lms, -2.0);

        Resultado r = CalculadoraCrecimiento.evaluar(lms, valor);

        assertThat(r.z()).isCloseTo(-2.0, within(1e-6));
        assertThat(r.percentil()).isCloseTo(2.28, within(0.05));
    }

    @Test
    void elPercentilTresCorrespondeAZMenosUnoPuntoOchentaYOcho() {
        PuntoLms lms = TABLA.get(0);
        double p3 = CalculadoraCrecimiento.valorEnZ(lms, CalculadoraCrecimiento.Z_PERCENTILES[0]);

        Resultado r = CalculadoraCrecimiento.evaluar(lms, p3);

        assertThat(r.percentil()).isCloseTo(3.0, within(0.1));
    }

    @Test
    void usaElLogaritmoCuandoLEsCero() {
        PuntoLms lms = new PuntoLms(6, 0.0, 10.0, 0.1);
        double valor = CalculadoraCrecimiento.valorEnZ(lms, 1.0);

        assertThat(valor).isCloseTo(10.0 * Math.exp(0.1), within(1e-9));
        assertThat(CalculadoraCrecimiento.evaluar(lms, valor).z()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void interpolaLinealmenteEntreLosPuntosDeLaTabla() {
        PuntoLms medio = CalculadoraCrecimiento.interpolar(TABLA, 6.0);

        assertThat(medio.m()).isCloseTo((3.3464 + 9.6479) / 2, within(1e-9));
        assertThat(medio.l()).isCloseTo((0.3487 + -0.0506) / 2, within(1e-9));
    }

    @Test
    void fueraDelRangoDeLaTablaNoHayReferencia() {
        assertThat(CalculadoraCrecimiento.interpolar(TABLA, 30.0)).isNull();
        assertThat(CalculadoraCrecimiento.interpolar(TABLA, -1.0)).isNull();
        assertThat(CalculadoraCrecimiento.interpolar(List.of(), 5.0)).isNull();
    }

    @Test
    void laDistribucionNormalCoincideConValoresConocidos() {
        assertThat(CalculadoraCrecimiento.distribucionNormal(0)).isCloseTo(0.5, within(1e-6));
        assertThat(CalculadoraCrecimiento.distribucionNormal(1.96)).isCloseTo(0.975, within(1e-3));
        assertThat(CalculadoraCrecimiento.distribucionNormal(-1.0)).isCloseTo(0.1587, within(1e-3));
        assertThat(CalculadoraCrecimiento.distribucionNormal(3.0)).isCloseTo(0.99865, within(1e-3));
    }

    @Test
    void clasificaPesoTallaYPerimetroPorDesviacionesEstandar() {
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.PESO_EDAD, 0.5)
                        .nivel())
                .isEqualTo(NivelAlerta.NORMAL);
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.TALLA_EDAD, -2.4)
                        .nivel())
                .isEqualTo(NivelAlerta.ATENCION);
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.PC_EDAD, 3.2)
                        .nivel())
                .isEqualTo(NivelAlerta.ALERTA);
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.PESO_EDAD, -3.1)
                        .nivel())
                .isEqualTo(NivelAlerta.ALERTA);
    }

    @Test
    void clasificaElImcDistinguiendoSobrepesoObesidadYDelgadez() {
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.IMC_EDAD, 2.5)
                        .mensaje())
                .contains("sobrepeso");
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.IMC_EDAD, 3.5)
                        .nivel())
                .isEqualTo(NivelAlerta.ALERTA);
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.IMC_EDAD, -2.5)
                        .mensaje())
                .contains("delgadez");
        assertThat(CalculadoraCrecimiento.clasificar(IndicadorCrecimiento.IMC_EDAD, 1.0)
                        .nivel())
                .isEqualTo(NivelAlerta.NORMAL);
    }

    @Test
    void detectaUnDescensoMarcadoEntreMedicionesConsecutivas() {
        Evaluacion cambio = CalculadoraCrecimiento.cambioBrusco(0.4, -0.8);

        assertThat(cambio).isNotNull();
        assertThat(cambio.nivel()).isEqualTo(NivelAlerta.ATENCION);
        assertThat(CalculadoraCrecimiento.cambioBrusco(0.4, 0.1)).isNull();
        assertThat(CalculadoraCrecimiento.cambioBrusco(-1.0, 0.5)).isNull();
    }

    @Test
    void convierteDiasEnMeses() {
        assertThat(CalculadoraCrecimiento.edadEnMeses(365)).isCloseTo(11.99, within(0.02));
        assertThat(CalculadoraCrecimiento.edadEnMeses(0)).isZero();
    }
}
