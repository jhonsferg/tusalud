package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.PeriodoReporte;
import com.codesoftlabs.tusalud.app.dto.response.ReporteMorbilidadResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import com.codesoftlabs.tusalud.app.repositories.AtencionDiagnosticoRepository;
import com.codesoftlabs.tusalud.app.repositories.AtencionDiagnosticoRepository.FilaMorbilidad;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MorbilidadCalculadora {

    public static final int UMBRAL_SUPRESION = 5;

    private static final String SIN_CAPITULO = "Sin capítulo";
    private static final int TOP_GENERAL = 15;
    private static final int TOP_ESPECIALIDAD = 5;
    private static final int TOP_TENDENCIA = 5;
    private static final List<String> GRUPOS = List.of(
            "0 a 4 años",
            "5 a 11 años",
            "12 a 17 años",
            "18 a 29 años",
            "30 a 59 años",
            "60 años o más",
            "Sin dato de edad");

    private final AtencionDiagnosticoRepository repositorio;

    private static final class Contador {
        String descripcion;
        String capitulo;
        long casos;
    }

    @Transactional(readOnly = true)
    public ReporteMorbilidadResponse calcular(PeriodoReporte periodo) {
        List<FilaMorbilidad> filas = repositorio.filasPrincipales(
                periodo.desde().atStartOfDay(), periodo.hasta().plusDays(1).atStartOfDay());
        long total = filas.size();

        Map<String, Contador> porCodigo = new HashMap<>();
        Map<String, Long> porCapitulo = new HashMap<>();
        Map<String, Map<String, Contador>> porEspecialidad = new LinkedHashMap<>();
        for (FilaMorbilidad fila : filas) {
            String capitulo =
                    fila.getCapitulo() == null || fila.getCapitulo().isBlank() ? SIN_CAPITULO : fila.getCapitulo();
            Contador contador =
                    porCodigo.computeIfAbsent(fila.getCodigo(), k -> nuevoContador(fila.getDescripcion(), capitulo));
            contador.casos++;
            porCapitulo.merge(capitulo, 1L, Long::sum);
            Contador deEspecialidad = porEspecialidad
                    .computeIfAbsent(fila.getEspecialidad(), k -> new HashMap<>())
                    .computeIfAbsent(fila.getCodigo(), k -> nuevoContador(fila.getDescripcion(), capitulo));
            deEspecialidad.casos++;
        }

        List<ReporteMorbilidadResponse.Diagnostico> top = porCodigo.entrySet().stream()
                .sorted(masFrecuente())
                .limit(TOP_GENERAL)
                .map(e -> new ReporteMorbilidadResponse.Diagnostico(
                        e.getKey(),
                        e.getValue().descripcion,
                        e.getValue().capitulo,
                        e.getValue().casos,
                        porcentaje(e.getValue().casos, total)))
                .toList();

        List<ReporteMorbilidadResponse.Capitulo> capitulos = porCapitulo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new ReporteMorbilidadResponse.Capitulo(
                        e.getKey(), e.getValue(), porcentaje(e.getValue(), total)))
                .toList();

        List<ReporteMorbilidadResponse.PorEspecialidad> especialidades = new ArrayList<>();
        porEspecialidad.forEach((nombre, codigos) -> {
            long casos = codigos.values().stream().mapToLong(c -> c.casos).sum();
            List<ReporteMorbilidadResponse.DiagnosticoCorto> cortos = codigos.entrySet().stream()
                    .sorted(masFrecuente())
                    .limit(TOP_ESPECIALIDAD)
                    .map(e -> new ReporteMorbilidadResponse.DiagnosticoCorto(
                            e.getKey(), e.getValue().descripcion, e.getValue().casos))
                    .toList();
            especialidades.add(new ReporteMorbilidadResponse.PorEspecialidad(nombre, casos, cortos));
        });
        especialidades.sort(Comparator.comparingLong(ReporteMorbilidadResponse.PorEspecialidad::casos)
                .reversed()
                .thenComparing(ReporteMorbilidadResponse.PorEspecialidad::especialidad));

        return new ReporteMorbilidadResponse(
                periodo,
                total,
                porCodigo.size(),
                UMBRAL_SUPRESION,
                top,
                capitulos,
                especialidades,
                grupos(filas),
                serie(periodo, filas),
                tendencia(periodo, filas, top));
    }

    private static Contador nuevoContador(String descripcion, String capitulo) {
        Contador c = new Contador();
        c.descripcion = descripcion;
        c.capitulo = capitulo;
        return c;
    }

    private static Comparator<Map.Entry<String, Contador>> masFrecuente() {
        return Comparator.<Map.Entry<String, Contador>>comparingLong(e -> e.getValue().casos)
                .reversed()
                .thenComparing(Map.Entry::getKey);
    }

    private static BigDecimal porcentaje(long casos, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(casos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static List<ReporteMorbilidadResponse.GrupoEtario> grupos(List<FilaMorbilidad> filas) {
        long[][] celdas = new long[GRUPOS.size()][3];
        for (FilaMorbilidad fila : filas) {
            int grupo = indiceGrupo(fila);
            int columna = fila.getSexo() == null ? 2 : fila.getSexo() == SexoPaciente.M ? 0 : 1;
            celdas[grupo][columna]++;
        }
        List<ReporteMorbilidadResponse.GrupoEtario> resultado = new ArrayList<>();
        for (int i = 0; i < GRUPOS.size(); i++) {
            long m = celdas[i][0];
            long f = celdas[i][1];
            long s = celdas[i][2];
            long suma = m + f + s;
            if (suma == 0) {
                resultado.add(new ReporteMorbilidadResponse.GrupoEtario(GRUPOS.get(i), 0L, 0L, 0L, 0L, false));
            } else if (suma < UMBRAL_SUPRESION) {
                resultado.add(new ReporteMorbilidadResponse.GrupoEtario(GRUPOS.get(i), null, null, null, null, true));
            } else {
                boolean oculto = pequena(m) || pequena(f) || pequena(s);
                resultado.add(new ReporteMorbilidadResponse.GrupoEtario(
                        GRUPOS.get(i), visible(m), visible(f), visible(s), oculto ? null : suma, oculto));
            }
        }
        return resultado;
    }

    private static boolean pequena(long valor) {
        return valor > 0 && valor < UMBRAL_SUPRESION;
    }

    private static Long visible(long valor) {
        return pequena(valor) ? null : valor;
    }

    private static int indiceGrupo(FilaMorbilidad fila) {
        if (fila.getNacimiento() == null) {
            return GRUPOS.size() - 1;
        }
        int edad = Math.max(
                0,
                Period.between(fila.getNacimiento(), fila.getFecha().toLocalDate())
                        .getYears());
        if (edad <= 4) {
            return 0;
        }
        if (edad <= 11) {
            return 1;
        }
        if (edad <= 17) {
            return 2;
        }
        if (edad <= 29) {
            return 3;
        }
        return edad <= 59 ? 4 : 5;
    }

    private static LocalDate cubeta(LocalDate fecha, AgrupacionReporte agrupacion) {
        return switch (agrupacion) {
            case DIA -> fecha;
            case SEMANA -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> fecha.withDayOfMonth(1);
        };
    }

    private static TreeMap<LocalDate, Long> cubetasVacias(PeriodoReporte periodo) {
        TreeMap<LocalDate, Long> cubetas = new TreeMap<>();
        for (int i = 0; i < periodo.dias(); i++) {
            cubetas.put(cubeta(periodo.desde().plusDays(i), periodo.agrupacion()), 0L);
        }
        return cubetas;
    }

    private static List<ReporteMorbilidadResponse.Punto> serie(PeriodoReporte periodo, List<FilaMorbilidad> filas) {
        TreeMap<LocalDate, Long> cubetas = cubetasVacias(periodo);
        for (FilaMorbilidad fila : filas) {
            cubetas.merge(cubeta(fila.getFecha().toLocalDate(), periodo.agrupacion()), 1L, Long::sum);
        }
        return puntos(cubetas);
    }

    private static List<ReporteMorbilidadResponse.Tendencia> tendencia(
            PeriodoReporte periodo, List<FilaMorbilidad> filas, List<ReporteMorbilidadResponse.Diagnostico> top) {
        List<ReporteMorbilidadResponse.Tendencia> resultado = new ArrayList<>();
        for (ReporteMorbilidadResponse.Diagnostico diagnostico :
                top.stream().limit(TOP_TENDENCIA).toList()) {
            TreeMap<LocalDate, Long> cubetas = cubetasVacias(periodo);
            for (FilaMorbilidad fila : filas) {
                if (diagnostico.codigo().equals(fila.getCodigo())) {
                    cubetas.merge(cubeta(fila.getFecha().toLocalDate(), periodo.agrupacion()), 1L, Long::sum);
                }
            }
            resultado.add(new ReporteMorbilidadResponse.Tendencia(
                    diagnostico.codigo(), diagnostico.descripcion(), puntos(cubetas)));
        }
        return resultado;
    }

    private static List<ReporteMorbilidadResponse.Punto> puntos(TreeMap<LocalDate, Long> cubetas) {
        return cubetas.entrySet().stream()
                .map(e -> new ReporteMorbilidadResponse.Punto(e.getKey(), e.getValue()))
                .toList();
    }
}
