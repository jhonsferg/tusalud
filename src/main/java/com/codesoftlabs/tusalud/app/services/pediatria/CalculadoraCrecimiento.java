package com.codesoftlabs.tusalud.app.services.pediatria;

import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.IndicadorCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.NivelAlerta;
import java.util.List;

public final class CalculadoraCrecimiento {

    public static final double[] Z_PERCENTILES = {-1.881, -1.036, 0.0, 1.036, 1.881};
    public static final int EDAD_MAXIMA_MESES = 228;
    private static final double DIAS_POR_MES = 30.4375;

    private CalculadoraCrecimiento() {}

    public record PuntoLms(int edadMeses, double l, double m, double s) {}

    public record Resultado(double z, double percentil) {}

    public record Evaluacion(NivelAlerta nivel, String mensaje) {}

    public static double edadEnMeses(long dias) {
        return dias / DIAS_POR_MES;
    }

    public static PuntoLms interpolar(List<PuntoLms> tabla, double edadMeses) {
        if (tabla == null || tabla.isEmpty()) {
            return null;
        }
        PuntoLms primero = tabla.get(0);
        PuntoLms ultimo = tabla.get(tabla.size() - 1);
        if (edadMeses < primero.edadMeses() || edadMeses > ultimo.edadMeses()) {
            return null;
        }
        for (int i = 0; i < tabla.size() - 1; i++) {
            PuntoLms a = tabla.get(i);
            PuntoLms b = tabla.get(i + 1);
            if (edadMeses >= a.edadMeses() && edadMeses <= b.edadMeses()) {
                double tramo = b.edadMeses() - a.edadMeses();
                double t = tramo == 0 ? 0 : (edadMeses - a.edadMeses()) / tramo;
                return new PuntoLms(
                        (int) Math.round(edadMeses),
                        a.l() + (b.l() - a.l()) * t,
                        a.m() + (b.m() - a.m()) * t,
                        a.s() + (b.s() - a.s()) * t);
            }
        }
        return ultimo;
    }

    public static Resultado evaluar(PuntoLms lms, double valor) {
        double z;
        if (Math.abs(lms.l()) < 1e-9) {
            z = Math.log(valor / lms.m()) / lms.s();
        } else {
            z = (Math.pow(valor / lms.m(), lms.l()) - 1) / (lms.l() * lms.s());
        }
        return new Resultado(z, distribucionNormal(z) * 100.0);
    }

    public static double valorEnZ(PuntoLms lms, double z) {
        if (Math.abs(lms.l()) < 1e-9) {
            return lms.m() * Math.exp(lms.s() * z);
        }
        return lms.m() * Math.pow(1 + lms.l() * lms.s() * z, 1.0 / lms.l());
    }

    public static double distribucionNormal(double z) {
        double x = Math.abs(z) / Math.sqrt(2.0);
        double t = 1.0 / (1.0 + 0.3275911 * x);
        double y = 1.0
                - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592)
                        * t
                        * Math.exp(-x * x);
        double erf = z >= 0 ? y : -y;
        return 0.5 * (1.0 + erf);
    }

    public static Evaluacion clasificar(IndicadorCrecimiento indicador, double z) {
        if (indicador == IndicadorCrecimiento.IMC_EDAD) {
            if (z > 3) {
                return new Evaluacion(NivelAlerta.ALERTA, "IMC muy por encima del rango esperado, posible obesidad");
            }
            if (z > 2) {
                return new Evaluacion(NivelAlerta.ATENCION, "IMC por encima del rango esperado, posible sobrepeso");
            }
            if (z < -3) {
                return new Evaluacion(
                        NivelAlerta.ALERTA, "IMC muy por debajo del rango esperado, posible delgadez severa");
            }
            if (z < -2) {
                return new Evaluacion(NivelAlerta.ATENCION, "IMC por debajo del rango esperado, posible delgadez");
            }
            return new Evaluacion(NivelAlerta.NORMAL, "Dentro del rango esperado");
        }
        String nombre = switch (indicador) {
            case PESO_EDAD -> "Peso";
            case TALLA_EDAD -> "Talla";
            default -> "Perímetro cefálico";
        };
        if (z > 3 || z < -3) {
            return new Evaluacion(
                    NivelAlerta.ALERTA,
                    nombre + " fuera de ±3 desviaciones estándar para la edad, requiere evaluación");
        }
        if (z > 2 || z < -2) {
            return new Evaluacion(
                    NivelAlerta.ATENCION, nombre + " fuera de ±2 desviaciones estándar para la edad, vigilar");
        }
        return new Evaluacion(NivelAlerta.NORMAL, "Dentro del rango esperado");
    }

    public static Evaluacion cambioBrusco(double zAnterior, double zActual) {
        double caida = zAnterior - zActual;
        if (caida >= 1.0) {
            return new Evaluacion(
                    NivelAlerta.ATENCION, "Descenso marcado respecto de la medición anterior, revisar la tendencia");
        }
        return null;
    }
}
