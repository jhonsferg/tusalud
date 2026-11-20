package com.codesoftlabs.tusalud.app.services.pediatria;

import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoDosis;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CalculadoraEsquemaVacunacion {

    public static final int TOLERANCIA_ATRASO_DIAS = 30;
    public static final int VENTANA_PROXIMA_DIAS = 30;

    private CalculadoraEsquemaVacunacion() {}

    public record DosisEsquema(
            UUID vacunaId,
            String vacunaCodigo,
            String vacunaNombre,
            int numeroDosis,
            String etiqueta,
            int edadMinimaDias,
            int edadRecomendadaDias,
            Integer edadMaximaDias,
            Integer intervaloMinimoDias) {}

    public record DosisAplicada(UUID vacunaId, int numeroDosis, LocalDate fecha, UUID registroId) {}

    public record DosisCalculada(
            DosisEsquema dosis,
            EstadoDosis estado,
            LocalDate fechaRecomendada,
            LocalDate fechaMinima,
            LocalDate fechaLimite,
            LocalDate fechaAplicada,
            UUID registroId,
            Integer atrasoDias) {}

    public static List<DosisCalculada> calcular(
            LocalDate nacimiento, LocalDate hoy, List<DosisEsquema> esquema, List<DosisAplicada> aplicadas) {
        return calcular(nacimiento, hoy, esquema, aplicadas, TOLERANCIA_ATRASO_DIAS);
    }

    public static List<DosisCalculada> calcular(
            LocalDate nacimiento,
            LocalDate hoy,
            List<DosisEsquema> esquema,
            List<DosisAplicada> aplicadas,
            int tolerancia) {
        Map<String, DosisAplicada> porClave = new HashMap<>();
        for (DosisAplicada a : aplicadas) {
            porClave.put(clave(a.vacunaId(), a.numeroDosis()), a);
        }
        List<DosisEsquema> ordenadas = new ArrayList<>(esquema);
        ordenadas.sort(Comparator.comparing(DosisEsquema::edadRecomendadaDias)
                .thenComparing(DosisEsquema::vacunaNombre)
                .thenComparingInt(DosisEsquema::numeroDosis));
        List<DosisCalculada> resultado = new ArrayList<>();
        for (DosisEsquema d : ordenadas) {
            DosisAplicada aplicada = porClave.get(clave(d.vacunaId(), d.numeroDosis()));
            LocalDate recomendada = nacimiento.plusDays(d.edadRecomendadaDias());
            LocalDate minima = nacimiento.plusDays(d.edadMinimaDias());
            LocalDate limite = d.edadMaximaDias() == null ? null : nacimiento.plusDays(d.edadMaximaDias());
            boolean previaBloquea = false;
            if (d.numeroDosis() > 1) {
                DosisAplicada previa = porClave.get(clave(d.vacunaId(), d.numeroDosis() - 1));
                if (previa == null) {
                    previaBloquea = true;
                } else if (d.intervaloMinimoDias() != null) {
                    LocalDate porIntervalo = previa.fecha().plusDays(d.intervaloMinimoDias());
                    if (porIntervalo.isAfter(recomendada)) {
                        recomendada = porIntervalo;
                    }
                    if (porIntervalo.isAfter(minima)) {
                        minima = porIntervalo;
                    }
                }
            }
            EstadoDosis estado;
            Integer atraso = null;
            if (aplicada != null) {
                estado = EstadoDosis.APLICADA;
            } else if (previaBloquea) {
                estado = EstadoDosis.PENDIENTE;
            } else if (limite != null && hoy.isAfter(limite)) {
                estado = EstadoDosis.FUERA_DE_EDAD;
            } else if (hoy.isAfter(recomendada.plusDays(tolerancia))) {
                estado = EstadoDosis.ATRASADA;
                atraso = (int) ChronoUnit.DAYS.between(recomendada, hoy);
            } else if (!recomendada.isAfter(hoy.plusDays(VENTANA_PROXIMA_DIAS))) {
                estado = EstadoDosis.PROXIMA;
            } else {
                estado = EstadoDosis.PENDIENTE;
            }
            resultado.add(new DosisCalculada(
                    d,
                    estado,
                    recomendada,
                    minima,
                    limite,
                    aplicada == null ? null : aplicada.fecha(),
                    aplicada == null ? null : aplicada.registroId(),
                    atraso));
        }
        return resultado;
    }

    public static DosisCalculada proxima(List<DosisCalculada> calculadas) {
        return calculadas.stream()
                .filter(c -> c.estado() == EstadoDosis.ATRASADA || c.estado() == EstadoDosis.PROXIMA)
                .min(Comparator.comparing(DosisCalculada::fechaRecomendada))
                .orElse(null);
    }

    public static long edadEnDias(LocalDate nacimiento, LocalDate hoy) {
        return ChronoUnit.DAYS.between(nacimiento, hoy);
    }

    public static int edadEnMeses(LocalDate nacimiento, LocalDate hoy) {
        return (int) ChronoUnit.MONTHS.between(nacimiento, hoy);
    }

    private static String clave(UUID vacunaId, int dosis) {
        return vacunaId + ":" + dosis;
    }
}
