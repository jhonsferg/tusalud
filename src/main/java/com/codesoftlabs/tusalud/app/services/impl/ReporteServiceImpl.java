package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.IndicadorReporte;
import com.codesoftlabs.tusalud.app.dto.response.PeriodoReporte;
import com.codesoftlabs.tusalud.app.dto.response.ReporteCalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteEjecutivoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteFinancieroResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteOperativoResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import com.codesoftlabs.tusalud.app.entities.enums.ClasificacionTriaje;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository.Cantidad;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository.CitaDia;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository.ComprobanteDia;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository.DispensacionDia;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository.Intervalo;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository.MontoDia;
import com.codesoftlabs.tusalud.app.repositories.ReporteRepository.MontoNombre;
import com.codesoftlabs.tusalud.app.services.interfaces.ReporteService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private static final int MAX_DIAS = 366;
    private static final int DIAS_POR_DEFECTO = 30;
    private static final int DIAS_POR_VENCER = 60;
    private static final int MAX_FILAS = 20;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ReporteRepository repositorio;
    private final MedicoRepository medicoRepository;

    private record Rango(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion) {

        LocalDateTime inicio() {
            return desde.atStartOfDay();
        }

        LocalDateTime fin() {
            return hasta.plusDays(1).atStartOfDay();
        }

        int dias() {
            return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
        }

        Rango anterior() {
            LocalDate finAnterior = desde.minusDays(1);
            return new Rango(finAnterior.minusDays(dias() - 1L), finAnterior, agrupacion);
        }

        PeriodoReporte periodo() {
            return new PeriodoReporte(desde, hasta, agrupacion, dias());
        }
    }

    private record Fuentes(
            List<ComprobanteDia> comprobantes, List<MontoDia> pagos, List<DispensacionDia> dispensaciones) {

        BigDecimal total() {
            return comprobantes.stream()
                    .map(ComprobanteDia::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(pagos.stream().map(MontoDia::total).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .add(dispensaciones.stream().map(DispensacionDia::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        long transacciones() {
            return comprobantes.stream()
                            .mapToLong(ComprobanteDia::transacciones)
                            .sum()
                    + pagos.stream().mapToLong(MontoDia::transacciones).sum()
                    + dispensaciones.stream()
                            .mapToLong(DispensacionDia::transacciones)
                            .sum();
        }
    }

    private record Metricas(
            long citas,
            long atendidas,
            long canceladas,
            BigDecimal tasaAusencia,
            BigDecimal ingresos,
            BigDecimal ticket,
            BigDecimal ocupacion,
            BigDecimal espera,
            BigDecimal valoracion) {}

    @Override
    public ReporteEjecutivoResponse ejecutivo(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion) {
        Rango actual = rango(desde, hasta, agrupacion);
        Rango previo = actual.anterior();
        Metricas a = metricas(actual);
        Metricas p = metricas(previo);

        Map<LocalDate, long[]> citasPorBucket = new TreeMap<>();
        for (CitaDia fila : repositorio.citasPorDiaYEstado(actual.inicio(), actual.fin())) {
            long[] c = citasPorBucket.computeIfAbsent(cubeta(fila.dia(), actual.agrupacion()), k -> new long[2]);
            c[0] += fila.total();
            if (fila.estado() == EstadoCita.ATENDIDA) {
                c[1] += fila.total();
            }
        }
        Map<LocalDate, BigDecimal> ingresosPorBucket = new TreeMap<>();
        Fuentes fuentes = fuentes(actual);
        acumularIngresos(fuentes, actual.agrupacion(), ingresosPorBucket, new TreeMap<>());
        TreeMap<LocalDate, ReporteEjecutivoResponse.Punto> serie = new TreeMap<>();
        for (LocalDate dia : cubetas(actual)) {
            long[] c = citasPorBucket.getOrDefault(dia, new long[2]);
            serie.put(
                    dia,
                    new ReporteEjecutivoResponse.Punto(
                            dia, c[0], c[1], escala(ingresosPorBucket.getOrDefault(dia, BigDecimal.ZERO))));
        }
        return new ReporteEjecutivoResponse(
                actual.periodo(),
                previo.periodo(),
                indicador(BigDecimal.valueOf(a.citas()), BigDecimal.valueOf(p.citas())),
                indicador(BigDecimal.valueOf(a.atendidas()), BigDecimal.valueOf(p.atendidas())),
                indicador(BigDecimal.valueOf(a.canceladas()), BigDecimal.valueOf(p.canceladas())),
                indicador(a.tasaAusencia(), p.tasaAusencia()),
                indicador(a.ingresos(), p.ingresos()),
                indicador(a.ticket(), p.ticket()),
                indicador(a.ocupacion(), p.ocupacion()),
                indicador(a.espera(), p.espera()),
                indicador(a.valoracion(), p.valoracion()),
                new ArrayList<>(serie.values()));
    }

    @Override
    public ReporteFinancieroResponse financiero(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion) {
        Rango r = rango(desde, hasta, agrupacion);
        Fuentes fuentes = fuentes(r);
        Map<LocalDate, BigDecimal> totalPorDia = new TreeMap<>();
        Map<LocalDate, Long> transPorDia = new TreeMap<>();
        acumularIngresos(fuentes, r.agrupacion(), totalPorDia, transPorDia);
        List<ReporteFinancieroResponse.Dia> porDia = new ArrayList<>();
        for (LocalDate dia : cubetas(r)) {
            porDia.add(new ReporteFinancieroResponse.Dia(
                    dia, escala(totalPorDia.getOrDefault(dia, BigDecimal.ZERO)), transPorDia.getOrDefault(dia, 0L)));
        }

        Map<String, BigDecimal> metodoTotal = new HashMap<>();
        Map<String, Long> metodoTrans = new HashMap<>();
        fuentes.comprobantes()
                .forEach(c -> sumar(metodoTotal, metodoTrans, c.metodo().name(), c.total(), c.transacciones()));
        fuentes.pagos()
                .forEach(p -> sumar(metodoTotal, metodoTrans, metodoEnLinea(p.clave()), p.total(), p.transacciones()));
        fuentes.dispensaciones()
                .forEach(x -> sumar(metodoTotal, metodoTrans, x.metodo().name(), x.total(), x.transacciones()));
        List<ReporteFinancieroResponse.Metodo> porMetodo = metodoTotal.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new ReporteFinancieroResponse.Metodo(
                        e.getKey(), escala(e.getValue()), metodoTrans.get(e.getKey())))
                .toList();

        BigDecimal consulta = suma(fuentes.comprobantes().stream()
                        .filter(c -> c.origen() == OrigenComprobante.CITA)
                        .map(ComprobanteDia::total))
                .add(suma(fuentes.pagos().stream().map(MontoDia::total)));
        BigDecimal laboratorio = suma(fuentes.comprobantes().stream()
                .filter(c -> c.origen() == OrigenComprobante.LABORATORIO)
                .map(ComprobanteDia::total));
        BigDecimal servicios = suma(fuentes.comprobantes().stream()
                .filter(c -> c.origen() == OrigenComprobante.SERVICIO || c.origen() == OrigenComprobante.OTRO)
                .map(ComprobanteDia::total));
        BigDecimal imagenes = suma(fuentes.comprobantes().stream()
                .filter(c -> c.origen() == OrigenComprobante.IMAGEN)
                .map(ComprobanteDia::total));
        BigDecimal farmacia = suma(fuentes.dispensaciones().stream().map(DispensacionDia::total));
        Cantidad hospital = repositorio.hospitalizacionDevengada(r.inicio(), r.fin());
        List<ReporteFinancieroResponse.Origen> porOrigen = List.of(
                new ReporteFinancieroResponse.Origen("CONSULTA", escala(consulta), false),
                new ReporteFinancieroResponse.Origen("FARMACIA", escala(farmacia), false),
                new ReporteFinancieroResponse.Origen("LABORATORIO", escala(laboratorio), false),
                new ReporteFinancieroResponse.Origen("IMAGENES", escala(imagenes), false),
                new ReporteFinancieroResponse.Origen("SERVICIOS", escala(servicios), false),
                new ReporteFinancieroResponse.Origen("HOSPITALIZACION", escala(hospital.monto()), true));

        long emitidos = repositorio.comprobantesPorEstado(r.inicio(), r.fin(), false);
        long anulados = repositorio.comprobantesPorEstado(r.inicio(), r.fin(), true);
        ReporteFinancieroResponse.Comprobantes comprobantes = new ReporteFinancieroResponse.Comprobantes(
                emitidos, anulados, porcentaje(BigDecimal.valueOf(anulados), BigDecimal.valueOf(emitidos + anulados)));

        Map<String, BigDecimal[]> cajeros = new TreeMap<>();
        Map<String, Long> sesiones = new HashMap<>();
        BigDecimal faltante = BigDecimal.ZERO;
        BigDecimal sobrante = BigDecimal.ZERO;
        long cerradas = 0;
        for (ReporteRepository.DiferenciaCaja f : repositorio.diferenciasDeCaja(r.inicio(), r.fin())) {
            BigDecimal[] acc =
                    cajeros.computeIfAbsent(f.cajero(), k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            sesiones.merge(f.cajero(), 1L, Long::sum);
            cerradas++;
            if (f.diferencia().signum() < 0) {
                acc[0] = acc[0].add(f.diferencia().abs());
                faltante = faltante.add(f.diferencia().abs());
            } else {
                acc[1] = acc[1].add(f.diferencia());
                sobrante = sobrante.add(f.diferencia());
            }
        }
        List<ReporteFinancieroResponse.Cajero> porCajero = cajeros.entrySet().stream()
                .map(e -> new ReporteFinancieroResponse.Cajero(
                        e.getKey(),
                        sesiones.get(e.getKey()),
                        escala(e.getValue()[0]),
                        escala(e.getValue()[1]),
                        escala(e.getValue()[1].subtract(e.getValue()[0]))))
                .toList();
        Cantidad citas = repositorio.citasPorCobrar();
        Cantidad lab = repositorio.laboratorioPorCobrar();
        ReporteFinancieroResponse.PorCobrar porCobrar = new ReporteFinancieroResponse.PorCobrar(
                new ReporteFinancieroResponse.Monto(citas.cantidad(), escala(citas.monto())),
                new ReporteFinancieroResponse.Monto(lab.cantidad(), escala(lab.monto())),
                escala(citas.monto().add(lab.monto())));

        BigDecimal total = fuentes.total();
        long transacciones = fuentes.transacciones();
        return new ReporteFinancieroResponse(
                r.periodo(),
                escala(total),
                transacciones,
                transacciones == 0
                        ? escala(BigDecimal.ZERO)
                        : escala(total.divide(BigDecimal.valueOf(transacciones), 4, RoundingMode.HALF_UP)),
                porDia,
                porMetodo,
                porOrigen,
                topItems(repositorio.pagosPorEspecialidad(r.inicio(), r.fin())),
                topItems(repositorio.pagosPorMedico(r.inicio(), r.fin())),
                comprobantes,
                new ReporteFinancieroResponse.Cajas(
                        cerradas, escala(faltante), escala(sobrante), escala(sobrante.subtract(faltante)), porCajero),
                porCobrar);
    }

    @Override
    public ReporteOperativoResponse operativo(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion) {
        Rango r = rango(desde, hasta, agrupacion);
        List<ReporteRepository.CamaEstado> camas = repositorio.camasPorAreaYEstado();
        long totalCamas =
                camas.stream().mapToLong(ReporteRepository.CamaEstado::total).sum();
        Map<AreaCama, long[]> porArea = new EnumMap<>(AreaCama.class);
        for (ReporteRepository.CamaEstado c : camas) {
            long[] v = porArea.computeIfAbsent(c.area(), k -> new long[5]);
            v[0] += c.total();
            v[idxEstado(c.estado())] += c.total();
        }
        List<ReporteOperativoResponse.AreaOcupacion> areas = porArea.entrySet().stream()
                .map(e -> new ReporteOperativoResponse.AreaOcupacion(
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[2],
                        e.getValue()[3],
                        e.getValue()[4],
                        porcentaje(BigDecimal.valueOf(e.getValue()[1]), BigDecimal.valueOf(e.getValue()[0]))))
                .toList();

        List<Intervalo> intervalos = repositorio.ingresosConCama(r.inicio(), r.fin());
        TreeMap<LocalDate, BigDecimal> ocupacionPorBucket = new TreeMap<>();
        Map<LocalDate, List<BigDecimal>> acumulado = new TreeMap<>();
        BigDecimal sumaDias = BigDecimal.ZERO;
        for (int i = 0; i < r.dias(); i++) {
            LocalDate dia = r.desde().plusDays(i);
            BigDecimal pct = porcentajeOcupacion(intervalos, dia, totalCamas);
            sumaDias = sumaDias.add(pct);
            acumulado
                    .computeIfAbsent(cubeta(dia, r.agrupacion()), k -> new ArrayList<>())
                    .add(pct);
        }
        acumulado.forEach((k, v) -> ocupacionPorBucket.put(
                k,
                escala(v.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(v.size()), 4, RoundingMode.HALF_UP))));
        List<ReporteOperativoResponse.PuntoOcupacion> serie = ocupacionPorBucket.entrySet().stream()
                .map(e -> new ReporteOperativoResponse.PuntoOcupacion(e.getKey(), e.getValue()))
                .toList();

        List<LocalDateTime[]> estancias = repositorio.estanciasConAlta(r.inicio(), r.fin());
        BigDecimal estancia = estancias.isEmpty()
                ? escala(BigDecimal.ZERO)
                : escala(BigDecimal.valueOf(estancias.stream()
                        .mapToDouble(e -> Duration.between(e[0], e[1]).toMinutes() / 1440.0)
                        .average()
                        .orElse(0)));

        List<Double> esperas = repositorio.esperasDeTurnos(r.inicio(), r.fin()).stream()
                .map(e -> Duration.between(e[0], e[1]).toSeconds() / 60.0)
                .filter(m -> m >= 0)
                .sorted()
                .toList();
        Map<Integer, List<Double>> esperaPorHora = new TreeMap<>();
        repositorio.esperasDeTurnos(r.inicio(), r.fin()).forEach(e -> {
            double m = Duration.between(e[0], e[1]).toSeconds() / 60.0;
            if (m >= 0) {
                esperaPorHora
                        .computeIfAbsent(e[0].getHour(), k -> new ArrayList<>())
                        .add(m);
            }
        });
        ReporteOperativoResponse.Esperas bloqueEsperas = new ReporteOperativoResponse.Esperas(
                esperas.size(),
                escala(BigDecimal.valueOf(esperas.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0))),
                escala(BigDecimal.valueOf(percentil(esperas, 0.9))),
                esperaPorHora.entrySet().stream()
                        .map(e -> new ReporteOperativoResponse.EsperaHora(
                                e.getKey(),
                                e.getValue().size(),
                                escala(BigDecimal.valueOf(e.getValue().stream()
                                        .mapToDouble(Double::doubleValue)
                                        .average()
                                        .orElse(0)))))
                        .toList());

        List<ReporteOperativoResponse.Emergencia> emergencias =
                repositorio.emergenciasPorNivel(r.inicio(), r.fin()).stream()
                        .sorted(Comparator.comparingInt(ReporteRepository.NivelConteo::nivel))
                        .map(n -> new ReporteOperativoResponse.Emergencia(
                                n.nivel(),
                                ClasificacionTriaje.deNivel(n.nivel()).name(),
                                n.total()))
                        .toList();

        Map<String, Long> demanda = new TreeMap<>();
        for (ReporteRepository.CitaHora c : repositorio.citasPorDiaYHora(r.inicio(), r.fin())) {
            demanda.merge(String.format("%d-%02d", c.dia().getDayOfWeek().getValue(), c.hora()), c.total(), Long::sum);
        }
        List<ReporteOperativoResponse.Demanda> celdas = demanda.entrySet().stream()
                .map(e -> new ReporteOperativoResponse.Demanda(
                        Integer.parseInt(e.getKey().substring(0, 1)),
                        Integer.parseInt(e.getKey().substring(2)),
                        e.getValue()))
                .toList();

        LocalDate hoy = LocalDate.now();
        List<ReporteRepository.ProductoStock> criticos = repositorio.productosEnStockCritico(hoy).stream()
                .sorted(Comparator.comparingDouble(p -> p.minimo() == 0 ? p.stock() : (double) p.stock() / p.minimo()))
                .limit(MAX_FILAS)
                .toList();
        Cantidad porVencer = repositorio.lotesPorVencer(hoy, hoy.plusDays(DIAS_POR_VENCER));
        Cantidad vencidos = repositorio.lotesVencidos(hoy);

        return new ReporteOperativoResponse(
                r.periodo(),
                escala(sumaDias.divide(BigDecimal.valueOf(r.dias()), 4, RoundingMode.HALF_UP)),
                areas,
                serie,
                estancias.size(),
                estancia,
                bloqueEsperas,
                emergencias,
                celdas,
                agenda(r),
                new ReporteOperativoResponse.Inventario(
                        criticos.stream()
                                .map(p -> new ReporteOperativoResponse.ProductoCritico(
                                        p.codigo(), p.nombre(), p.stock(), p.minimo()))
                                .toList(),
                        porVencer.cantidad(),
                        porVencer.monto().longValue(),
                        vencidos.cantidad(),
                        vencidos.monto().longValue()));
    }

    @Override
    public ReporteCalidadResponse calidad(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion) {
        Rango r = rango(desde, hasta, agrupacion);
        LocalDateTime ahora = LocalDateTime.now();
        Object[] val = repositorio.valoracionPromedio(r.inicio(), r.fin());
        long totalVal = ((Number) val[1]).longValue();
        long[] distribucionGlobal = new long[5];

        Map<UUID, long[]> porMedico = new HashMap<>();
        Map<UUID, String> nombres = new HashMap<>();
        for (ReporteRepository.ValoracionMedico v : repositorio.valoracionesPorMedico(r.inicio(), r.fin())) {
            porMedico.computeIfAbsent(v.medicoId(), k -> new long[5])[indicePuntuacion(v.puntuacion())] += v.total();
            nombres.put(v.medicoId(), v.nombre());
            distribucionGlobal[indicePuntuacion(v.puntuacion())] += v.total();
        }
        Map<String, long[]> porEspecialidad = new TreeMap<>();
        for (ReporteRepository.ValoracionEspecialidad v :
                repositorio.valoracionesPorEspecialidad(r.inicio(), r.fin())) {
            porEspecialidad.computeIfAbsent(v.especialidad(), k -> new long[5])[indicePuntuacion(v.puntuacion())] +=
                    v.total();
        }
        List<ReporteCalidadResponse.ValoracionGrupo> gruposMedico = porMedico.entrySet().stream()
                .map(e -> grupo(nombres.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(ReporteCalidadResponse.ValoracionGrupo::promedio)
                        .reversed())
                .toList();
        List<ReporteCalidadResponse.ValoracionGrupo> gruposEspecialidad = porEspecialidad.entrySet().stream()
                .map(e -> grupo(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ReporteCalidadResponse.ValoracionGrupo::promedio)
                        .reversed())
                .toList();

        Map<String, long[]> citasEsp = new TreeMap<>();
        for (ReporteRepository.CitaEspecialidad c : repositorio.citasPorEspecialidadYEstado(r.inicio(), r.fin())) {
            long[] v = citasEsp.computeIfAbsent(c.especialidad(), k -> new long[3]);
            v[0] += c.total();
            if (c.estado() == EstadoCita.CANCELADA) {
                v[1] += c.total();
            }
        }
        repositorio
                .citasSinAtenderPasadasPorEspecialidad(r.inicio(), r.fin(), ahora)
                .forEach(c -> citasEsp.computeIfAbsent(c.nombre(), k -> new long[3])[2] += c.total());
        List<ReporteCalidadResponse.Cancelacion> cancelaciones = citasEsp.entrySet().stream()
                .map(e -> new ReporteCalidadResponse.Cancelacion(
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[2],
                        porcentaje(BigDecimal.valueOf(e.getValue()[1]), BigDecimal.valueOf(e.getValue()[0])),
                        porcentaje(
                                BigDecimal.valueOf(e.getValue()[2]),
                                BigDecimal.valueOf(e.getValue()[0] - e.getValue()[1]))))
                .toList();

        List<ReporteRepository.OrdenLab> ordenes = repositorio.ordenesCompletadas(r.inicio(), r.fin());
        List<Double> horas = ordenes.stream()
                .map(o -> Duration.between(o.fecha(), o.completada()).toMinutes() / 60.0)
                .filter(h -> h >= 0)
                .sorted()
                .toList();
        Map<String, List<Double>> porPrioridad = new TreeMap<>();
        ordenes.forEach(o -> {
            double h = Duration.between(o.fecha(), o.completada()).toMinutes() / 60.0;
            if (h >= 0) {
                porPrioridad
                        .computeIfAbsent(o.prioridad().name(), k -> new ArrayList<>())
                        .add(h);
            }
        });
        ReporteCalidadResponse.Laboratorio laboratorio = new ReporteCalidadResponse.Laboratorio(
                ordenes.size(),
                repositorio.ordenesCanceladas(r.inicio(), r.fin()),
                escala(BigDecimal.valueOf(horas.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0))),
                escala(BigDecimal.valueOf(percentil(horas, 0.9))),
                porPrioridad.entrySet().stream()
                        .map(e -> new ReporteCalidadResponse.PorPrioridad(
                                e.getKey(),
                                e.getValue().size(),
                                escala(BigDecimal.valueOf(e.getValue().stream()
                                        .mapToDouble(Double::doubleValue)
                                        .average()
                                        .orElse(0)))))
                        .toList());

        List<ReporteCalidadResponse.Motivo> motivos =
                agruparMotivos(repositorio.motivosAnulacionComprobantes(r.inicio(), r.fin()));
        long anulados = motivos.stream()
                .mapToLong(ReporteCalidadResponse.Motivo::cantidad)
                .sum();
        long dispensacionesAnuladas = repositorio.dispensacionesAnuladas(r.inicio(), r.fin());
        List<ReporteCalidadResponse.Motivo> motivosDispensaciones = dispensacionesAnuladas == 0
                ? List.of()
                : List.of(new ReporteCalidadResponse.Motivo("Sin motivo registrado", dispensacionesAnuladas));

        BigDecimal promedio =
                val[0] == null ? escala(BigDecimal.ZERO) : escala(BigDecimal.valueOf(((Number) val[0]).doubleValue()));
        return new ReporteCalidadResponse(
                r.periodo(),
                new ReporteCalidadResponse.Valoraciones(promedio, totalVal, listaDe(distribucionGlobal)),
                gruposMedico,
                gruposEspecialidad,
                cancelaciones,
                laboratorio,
                new ReporteCalidadResponse.Anulaciones(
                        anulados, dispensacionesAnuladas, motivos, motivosDispensaciones));
    }

    private Metricas metricas(Rango r) {
        long citas = 0;
        long atendidas = 0;
        long canceladas = 0;
        for (CitaDia f : repositorio.citasPorDiaYEstado(r.inicio(), r.fin())) {
            citas += f.total();
            if (f.estado() == EstadoCita.ATENDIDA) {
                atendidas += f.total();
            } else if (f.estado() == EstadoCita.CANCELADA) {
                canceladas += f.total();
            }
        }
        long ausentes = repositorio.citasSinAtenderPasadasPorDia(r.inicio(), r.fin(), LocalDateTime.now()).stream()
                .mapToLong(ReporteRepository.ConteoDia::total)
                .sum();
        Fuentes fuentes = fuentes(r);
        BigDecimal ingresos = fuentes.total();
        long transacciones = fuentes.transacciones();
        List<Double> esperas = repositorio.esperasDeTurnos(r.inicio(), r.fin()).stream()
                .map(e -> Duration.between(e[0], e[1]).toSeconds() / 60.0)
                .filter(m -> m >= 0)
                .toList();
        Object[] val = repositorio.valoracionPromedio(r.inicio(), r.fin());
        long totalCamas = repositorio.camasPorAreaYEstado().stream()
                .mapToLong(ReporteRepository.CamaEstado::total)
                .sum();
        List<Intervalo> intervalos = repositorio.ingresosConCama(r.inicio(), r.fin());
        BigDecimal sumaOcupacion = BigDecimal.ZERO;
        for (int i = 0; i < r.dias(); i++) {
            sumaOcupacion =
                    sumaOcupacion.add(porcentajeOcupacion(intervalos, r.desde().plusDays(i), totalCamas));
        }
        return new Metricas(
                citas,
                atendidas,
                canceladas,
                porcentaje(BigDecimal.valueOf(ausentes), BigDecimal.valueOf(atendidas + ausentes)),
                escala(ingresos),
                transacciones == 0
                        ? escala(BigDecimal.ZERO)
                        : escala(ingresos.divide(BigDecimal.valueOf(transacciones), 4, RoundingMode.HALF_UP)),
                escala(sumaOcupacion.divide(BigDecimal.valueOf(r.dias()), 4, RoundingMode.HALF_UP)),
                escala(BigDecimal.valueOf(esperas.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0))),
                val[0] == null ? escala(BigDecimal.ZERO) : escala(BigDecimal.valueOf(((Number) val[0]).doubleValue())));
    }

    private Fuentes fuentes(Rango r) {
        return new Fuentes(
                repositorio.comprobantesEmitidosPorDia(r.inicio(), r.fin()),
                repositorio.pagosEnLineaPorDia(r.inicio(), r.fin()),
                repositorio.dispensacionesPorDia(r.inicio(), r.fin()));
    }

    private void acumularIngresos(
            Fuentes f,
            AgrupacionReporte agrupacion,
            Map<LocalDate, BigDecimal> totales,
            Map<LocalDate, Long> transacciones) {
        f.comprobantes()
                .forEach(c ->
                        acumular(totales, transacciones, cubeta(c.dia(), agrupacion), c.total(), c.transacciones()));
        f.pagos()
                .forEach(p ->
                        acumular(totales, transacciones, cubeta(p.dia(), agrupacion), p.total(), p.transacciones()));
        f.dispensaciones()
                .forEach(x ->
                        acumular(totales, transacciones, cubeta(x.dia(), agrupacion), x.total(), x.transacciones()));
    }

    private List<ReporteOperativoResponse.AgendaMedico> agenda(Rango r) {
        Map<UUID, Long> capacidad = new HashMap<>();
        for (ReporteRepository.HorarioFila h : repositorio.horarios()) {
            long huecos = Math.max(0, Duration.between(h.inicio(), h.fin()).toMinutes() / Math.max(1, h.duracion()));
            long dias = 0;
            for (int i = 0; i < r.dias(); i++) {
                if (r.desde().plusDays(i).getDayOfWeek() == h.dia()) {
                    dias++;
                }
            }
            capacidad.merge(h.medicoId(), huecos * dias, Long::sum);
        }
        Map<UUID, Long> citas = new HashMap<>();
        repositorio.citasActivasPorMedico(r.inicio(), r.fin()).forEach(c -> citas.put(c.medicoId(), c.total()));
        List<ReporteOperativoResponse.AgendaMedico> filas = new ArrayList<>();
        for (Medico m : medicoRepository.findAllWithDetails()) {
            long cap = capacidad.getOrDefault(m.getId(), 0L);
            long agendadas = citas.getOrDefault(m.getId(), 0L);
            if (cap == 0 && agendadas == 0) {
                continue;
            }
            filas.add(new ReporteOperativoResponse.AgendaMedico(
                    m.getUsuario().getNombreCompleto(),
                    agendadas,
                    cap,
                    porcentaje(BigDecimal.valueOf(agendadas), BigDecimal.valueOf(cap))));
        }
        filas.sort(Comparator.comparing(ReporteOperativoResponse.AgendaMedico::utilizacionPct)
                .reversed());
        return filas;
    }

    private static BigDecimal porcentajeOcupacion(List<Intervalo> intervalos, LocalDate dia, long totalCamas) {
        if (totalCamas == 0) {
            return escala(BigDecimal.ZERO);
        }
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        long ocupadas = intervalos.stream()
                .filter(i ->
                        i.inicio().isBefore(fin) && (i.fin() == null || !i.fin().isBefore(inicio)))
                .count();
        return porcentaje(BigDecimal.valueOf(Math.min(ocupadas, totalCamas)), BigDecimal.valueOf(totalCamas));
    }

    private static int idxEstado(EstadoCama estado) {
        return switch (estado) {
            case OCUPADA -> 1;
            case LIBRE -> 2;
            case LIMPIEZA -> 3;
            case MANTENIMIENTO -> 4;
        };
    }

    private static ReporteCalidadResponse.ValoracionGrupo grupo(String nombre, long[] distribucion) {
        long total = 0;
        long suma = 0;
        for (int i = 0; i < 5; i++) {
            total += distribucion[i];
            suma += distribucion[i] * (i + 1L);
        }
        BigDecimal promedio = total == 0
                ? escala(BigDecimal.ZERO)
                : BigDecimal.valueOf(suma).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new ReporteCalidadResponse.ValoracionGrupo(nombre, total, promedio, listaDe(distribucion));
    }

    private static List<ReporteCalidadResponse.Motivo> agruparMotivos(List<ReporteRepository.ConteoNombre> filas) {
        Map<String, Long> agrupado = new LinkedHashMap<>();
        Map<String, String> etiqueta = new HashMap<>();
        for (ReporteRepository.ConteoNombre f : filas) {
            String texto = f.nombre() == null || f.nombre().isBlank()
                    ? "Sin motivo"
                    : f.nombre().trim();
            if (texto.length() > 60) {
                texto = texto.substring(0, 60);
            }
            String clave = texto.toLowerCase(Locale.ROOT);
            etiqueta.putIfAbsent(clave, texto);
            agrupado.merge(clave, f.total(), Long::sum);
        }
        return agrupado.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new ReporteCalidadResponse.Motivo(etiqueta.get(e.getKey()), e.getValue()))
                .toList();
    }

    private static List<ReporteFinancieroResponse.Item> topItems(List<MontoNombre> filas) {
        return filas.stream()
                .sorted(Comparator.comparing(MontoNombre::total).reversed())
                .limit(MAX_FILAS)
                .map(f -> new ReporteFinancieroResponse.Item(f.nombre(), f.cantidad(), escala(f.total())))
                .toList();
    }

    private static void sumar(
            Map<String, BigDecimal> totales,
            Map<String, Long> transacciones,
            String clave,
            BigDecimal monto,
            long cantidad) {
        totales.merge(clave, monto, BigDecimal::add);
        transacciones.merge(clave, cantidad, Long::sum);
    }

    private static void acumular(
            Map<LocalDate, BigDecimal> totales,
            Map<LocalDate, Long> transacciones,
            LocalDate clave,
            BigDecimal monto,
            long cantidad) {
        totales.merge(clave, monto, BigDecimal::add);
        transacciones.merge(clave, cantidad, Long::sum);
    }

    private static BigDecimal suma(java.util.stream.Stream<BigDecimal> montos) {
        return montos.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String metodoEnLinea(String descripcion) {
        String texto = descripcion == null ? "" : descripcion.toUpperCase(Locale.ROOT);
        if (texto.contains("YAPE")) {
            return "YAPE";
        }
        if (texto.contains("PLIN")) {
            return "PLIN";
        }
        return "TARJETA";
    }

    private static IndicadorReporte indicador(BigDecimal actual, BigDecimal anterior) {
        BigDecimal variacion = anterior.signum() == 0
                ? null
                : actual.subtract(anterior).multiply(CIEN).divide(anterior, 2, RoundingMode.HALF_UP);
        return new IndicadorReporte(escala(actual), escala(anterior), variacion);
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return escala(BigDecimal.ZERO);
        }
        return parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static double percentil(List<Double> ordenados, double p) {
        if (ordenados.isEmpty()) {
            return 0;
        }
        int indice = (int) Math.ceil(p * ordenados.size()) - 1;
        return ordenados.get(Math.max(0, Math.min(indice, ordenados.size() - 1)));
    }

    private static int indicePuntuacion(int puntuacion) {
        return Math.max(1, Math.min(5, puntuacion)) - 1;
    }

    private static List<Long> listaDe(long[] valores) {
        List<Long> lista = new ArrayList<>(valores.length);
        for (long v : valores) {
            lista.add(v);
        }
        return lista;
    }

    private static LocalDate cubeta(LocalDate dia, AgrupacionReporte agrupacion) {
        return switch (agrupacion) {
            case DIA -> dia;
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> dia.withDayOfMonth(1);
        };
    }

    private static List<LocalDate> cubetas(Rango r) {
        TreeMap<LocalDate, Boolean> claves = new TreeMap<>();
        for (int i = 0; i < r.dias(); i++) {
            claves.put(cubeta(r.desde().plusDays(i), r.agrupacion()), Boolean.TRUE);
        }
        return new ArrayList<>(claves.keySet());
    }

    private static Rango rango(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO - 1L) : desde;
        if (inicio.isAfter(fin)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAX_DIAS) {
            throw new DatosInvalidosException("El rango del reporte no puede superar " + MAX_DIAS + " días");
        }
        return new Rango(inicio, fin, agrupacion == null ? AgrupacionReporte.DIA : agrupacion);
    }
}
