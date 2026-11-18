package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.dto.response.IndicadorReporte;
import com.codesoftlabs.tusalud.app.dto.response.PeriodoReporte;
import com.codesoftlabs.tusalud.app.dto.response.ReporteCalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteEjecutivoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteFinancieroResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteOperativoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import com.codesoftlabs.tusalud.app.services.interfaces.ReporteService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentosReportes {

    private static final String SIN_DATO = "-";

    private final ReporteService reporteService;

    public ReporteArmado armar(
            TipoDocumento tipo, LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion, boolean conGraficos) {
        return switch (tipo) {
            case REPORTE_EJECUTIVO -> ejecutivo(reporteService.ejecutivo(desde, hasta, agrupacion), conGraficos);
            case REPORTE_FINANCIERO -> financiero(reporteService.financiero(desde, hasta, agrupacion), conGraficos);
            case REPORTE_OPERATIVO -> operativo(reporteService.operativo(desde, hasta, agrupacion), conGraficos);
            case REPORTE_CALIDAD -> calidad(reporteService.calidad(desde, hasta, agrupacion), conGraficos);
            default -> throw new IllegalArgumentException("El documento no es un reporte: " + tipo);
        };
    }

    private ReporteArmado ejecutivo(ReporteEjecutivoResponse r, boolean graficos) {
        FichaDocumento ficha =
                base(TipoDocumento.REPORTE_EJECUTIVO, "Reporte ejecutivo", r.periodo(), r.periodoAnterior());
        ficha.seccion("Indicadores").cabecera("Indicador", "Actual", "Anterior", "Variación");
        HojaExcel indicadores = HojaExcel.de("Indicadores", "Indicador", "Actual", "Anterior", "Variación %");
        List<Object[]> lineas = List.of(
                linea("Citas", r.citas(), v -> Formato.numero(v, 0)),
                linea("Atendidas", r.atendidas(), v -> Formato.numero(v, 0)),
                linea("Canceladas", r.canceladas(), v -> Formato.numero(v, 0)),
                linea("Tasa de ausencia", r.tasaAusenciaPct(), Formato::porcentaje),
                linea("Ingresos", r.ingresos(), Formato::moneda),
                linea("Ticket promedio", r.ticketPromedio(), Formato::moneda),
                linea("Ocupación hospitalaria", r.ocupacionHospitalariaPct(), Formato::porcentaje),
                linea("Espera promedio en minutos", r.esperaPromedioMin(), v -> Formato.numero(v, 1)),
                linea("Valoración promedio", r.valoracionPromedio(), v -> Formato.numero(v, 2)));
        for (Object[] linea : lineas) {
            IndicadorReporte indicador = (IndicadorReporte) linea[1];
            ficha.fila((String) linea[0], (String) linea[2], (String) linea[3], variacion(indicador));
            indicadores.fila(linea[0], indicador.actual(), indicador.anterior(), indicador.variacionPct());
        }
        ficha.seccion("Serie por " + unidad(r.periodo().agrupacion()))
                .cabecera("Fecha", "Citas", "Atendidas", "Ingresos");
        HojaExcel serie = HojaExcel.de("Serie", "Fecha", "Citas", "Atendidas", "Ingresos");
        List<String> etiquetas = new ArrayList<>();
        List<Number> citas = new ArrayList<>();
        List<Number> ingresos = new ArrayList<>();
        for (ReporteEjecutivoResponse.Punto punto : r.serie()) {
            ficha.fila(
                    Formato.fecha(punto.fecha()),
                    Formato.numero((double) punto.citas(), 0),
                    Formato.numero((double) punto.atendidas(), 0),
                    Formato.moneda(punto.ingresos()));
            serie.fila(punto.fecha(), punto.citas(), punto.atendidas(), punto.ingresos());
            etiquetas.add(corta(punto.fecha()));
            citas.add(punto.citas());
            ingresos.add(punto.ingresos());
        }
        if (graficos && !etiquetas.isEmpty()) {
            ficha.grafico(1, GraficosPdf.lineas("Citas", etiquetas, citas));
            ficha.grafico(2, GraficosPdf.barras("Ingresos", etiquetas, ingresos));
        }
        return new ReporteArmado(
                ficha, List.of(indicadores, serie), "Reporte ejecutivo", nombre("ejecutivo", r.periodo()));
    }

    private ReporteArmado financiero(ReporteFinancieroResponse r, boolean graficos) {
        FichaDocumento ficha = base(TipoDocumento.REPORTE_FINANCIERO, "Reporte financiero", r.periodo(), null);
        HojaExcel resumen = HojaExcel.de("Resumen", "Concepto", "Valor");
        ficha.seccion("Resumen").cabecera("Concepto", "Valor");
        agregarResumen(ficha, resumen, "Total cobrado", r.totalCobrado(), Formato.moneda(r.totalCobrado()));
        agregarResumen(
                ficha, resumen, "Transacciones", r.transacciones(), Formato.numero((double) r.transacciones(), 0));
        agregarResumen(ficha, resumen, "Ticket promedio", r.ticketPromedio(), Formato.moneda(r.ticketPromedio()));
        agregarResumen(
                ficha,
                resumen,
                "Comprobantes emitidos",
                r.comprobantes().emitidos(),
                Formato.numero((double) r.comprobantes().emitidos(), 0));
        agregarResumen(
                ficha,
                resumen,
                "Comprobantes anulados",
                r.comprobantes().anulados(),
                Formato.numero((double) r.comprobantes().anulados(), 0));
        agregarResumen(
                ficha,
                resumen,
                "Tasa de anulación",
                r.comprobantes().tasaAnulacionPct(),
                Formato.porcentaje(r.comprobantes().tasaAnulacionPct()));
        agregarResumen(
                ficha,
                resumen,
                "Cajas cerradas",
                r.cajas().sesionesCerradas(),
                Formato.numero((double) r.cajas().sesionesCerradas(), 0));
        agregarResumen(
                ficha,
                resumen,
                "Faltante de caja",
                r.cajas().faltante(),
                Formato.moneda(r.cajas().faltante()));
        agregarResumen(
                ficha,
                resumen,
                "Sobrante de caja",
                r.cajas().sobrante(),
                Formato.moneda(r.cajas().sobrante()));
        agregarResumen(
                ficha,
                resumen,
                "Por cobrar",
                r.porCobrar().total(),
                Formato.moneda(r.porCobrar().total()));

        ficha.seccion("Ingresos por día").cabecera("Fecha", "Total", "Transacciones");
        HojaExcel dias = HojaExcel.de("Por día", "Fecha", "Total", "Transacciones");
        List<String> etiquetas = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        for (ReporteFinancieroResponse.Dia dia : r.porDia()) {
            ficha.fila(
                    Formato.fecha(dia.fecha()),
                    Formato.moneda(dia.total()),
                    Formato.numero((double) dia.transacciones(), 0));
            dias.fila(dia.fecha(), dia.total(), dia.transacciones());
            etiquetas.add(corta(dia.fecha()));
            valores.add(dia.total());
        }
        ficha.seccion("Por método de pago").cabecera("Método", "Total", "Transacciones");
        HojaExcel metodos = HojaExcel.de("Por método", "Método", "Total", "Transacciones");
        List<String> etiquetasMetodo = new ArrayList<>();
        List<Number> valoresMetodo = new ArrayList<>();
        for (ReporteFinancieroResponse.Metodo metodo : r.porMetodo()) {
            String nombre = Etiquetas.metodoPago(metodo.metodo());
            ficha.fila(nombre, Formato.moneda(metodo.total()), Formato.numero((double) metodo.transacciones(), 0));
            metodos.fila(nombre, metodo.total(), metodo.transacciones());
            etiquetasMetodo.add(nombre);
            valoresMetodo.add(metodo.total());
        }
        ficha.seccion("Por origen").cabecera("Origen", "Total", "Tipo");
        HojaExcel origenes = HojaExcel.de("Por origen", "Origen", "Total", "Tipo");
        for (ReporteFinancieroResponse.Origen origen : r.porOrigen()) {
            String nombre = Etiquetas.origen(origen.origen());
            String tipo = origen.devengado() ? "Devengado" : "Cobrado";
            ficha.fila(nombre, Formato.moneda(origen.total()), tipo);
            origenes.fila(nombre, origen.total(), tipo);
        }
        HojaExcel especialidades = tablaItems(ficha, "Por especialidad", r.porEspecialidad(), "Especialidad");
        HojaExcel medicos = tablaItems(ficha, "Por médico", r.porMedico(), "Médico");
        ficha.seccion("Diferencias de caja por cajero").cabecera("Cajero", "Sesiones", "Faltante", "Sobrante", "Neto");
        HojaExcel cajeros = HojaExcel.de("Cajas", "Cajero", "Sesiones", "Faltante", "Sobrante", "Neto");
        for (ReporteFinancieroResponse.Cajero cajero : r.cajas().porCajero()) {
            ficha.fila(
                    cajero.nombre(),
                    Formato.numero((double) cajero.sesiones(), 0),
                    Formato.moneda(cajero.faltante()),
                    Formato.moneda(cajero.sobrante()),
                    Formato.moneda(cajero.neto()));
            cajeros.fila(cajero.nombre(), cajero.sesiones(), cajero.faltante(), cajero.sobrante(), cajero.neto());
        }
        if (graficos) {
            if (!etiquetas.isEmpty()) {
                ficha.grafico(1, GraficosPdf.barras("Ingresos por día", etiquetas, valores));
            }
            if (!etiquetasMetodo.isEmpty()) {
                ficha.grafico(2, GraficosPdf.torta("Por método de pago", etiquetasMetodo, valoresMetodo));
            }
        }
        return new ReporteArmado(
                ficha,
                List.of(resumen, dias, metodos, origenes, especialidades, medicos, cajeros),
                "Reporte financiero",
                nombre("financiero", r.periodo()));
    }

    private ReporteArmado operativo(ReporteOperativoResponse r, boolean graficos) {
        FichaDocumento ficha = base(TipoDocumento.REPORTE_OPERATIVO, "Reporte operativo", r.periodo(), null);
        HojaExcel resumen = HojaExcel.de("Resumen", "Concepto", "Valor");
        ficha.seccion("Resumen").cabecera("Concepto", "Valor");
        agregarResumen(
                ficha,
                resumen,
                "Ocupación promedio",
                r.ocupacionPromedioPct(),
                Formato.porcentaje(r.ocupacionPromedioPct()));
        agregarResumen(ficha, resumen, "Altas", r.altas(), Formato.numero((double) r.altas(), 0));
        agregarResumen(
                ficha,
                resumen,
                "Estancia promedio en días",
                r.estanciaPromedioDias(),
                Formato.numero(r.estanciaPromedioDias(), 1));
        agregarResumen(
                ficha,
                resumen,
                "Turnos atendidos",
                r.esperas().turnos(),
                Formato.numero((double) r.esperas().turnos(), 0));
        agregarResumen(
                ficha,
                resumen,
                "Espera promedio en minutos",
                r.esperas().promedioMin(),
                Formato.numero(r.esperas().promedioMin(), 1));
        agregarResumen(
                ficha,
                resumen,
                "Espera percentil 90 en minutos",
                r.esperas().p90Min(),
                Formato.numero(r.esperas().p90Min(), 1));

        ficha.seccion("Camas por área").cabecera("Área", "Total", "Ocupadas", "Libres", "Limpieza", "Ocupación");
        HojaExcel camas = HojaExcel.de(
                "Camas", "Área", "Total", "Ocupadas", "Libres", "Limpieza", "Mantenimiento", "Ocupación %");
        List<String> etiquetasArea = new ArrayList<>();
        List<Number> valoresArea = new ArrayList<>();
        for (ReporteOperativoResponse.AreaOcupacion area : r.camasActuales()) {
            ficha.fila(
                    area.area().name(),
                    String.valueOf(area.total()),
                    String.valueOf(area.ocupadas()),
                    String.valueOf(area.libres()),
                    String.valueOf(area.limpieza()),
                    Formato.porcentaje(area.porcentaje()));
            camas.fila(
                    area.area().name(),
                    area.total(),
                    area.ocupadas(),
                    area.libres(),
                    area.limpieza(),
                    area.mantenimiento(),
                    area.porcentaje());
            etiquetasArea.add(area.area().name());
            valoresArea.add(area.porcentaje());
        }
        ficha.seccion("Esperas por hora del día").cabecera("Hora", "Turnos", "Promedio en minutos");
        HojaExcel esperas = HojaExcel.de("Esperas", "Hora", "Turnos", "Promedio en minutos");
        for (ReporteOperativoResponse.EsperaHora hora : r.esperas().porHora()) {
            ficha.fila(hora.hora() + ":00", String.valueOf(hora.turnos()), Formato.numero(hora.promedioMin(), 1));
            esperas.fila(hora.hora() + ":00", hora.turnos(), hora.promedioMin());
        }
        ficha.seccion("Emergencias por nivel de triaje").cabecera("Nivel", "Clasificación", "Cantidad");
        HojaExcel emergencias = HojaExcel.de("Emergencias", "Nivel", "Clasificación", "Cantidad");
        for (ReporteOperativoResponse.Emergencia emergencia : r.emergenciasPorNivel()) {
            ficha.fila(
                    String.valueOf(emergencia.nivel()),
                    emergencia.clasificacion(),
                    String.valueOf(emergencia.cantidad()));
            emergencias.fila(emergencia.nivel(), emergencia.clasificacion(), emergencia.cantidad());
        }
        HojaExcel demanda = HojaExcel.de("Demanda", "Día de la semana", "Hora", "Citas");
        long[] porDia = new long[8];
        for (ReporteOperativoResponse.Demanda celda : r.demanda()) {
            demanda.fila(nombreDia(celda.diaSemana()), celda.hora() + ":00", celda.citas());
            porDia[celda.diaSemana()] += celda.citas();
        }
        ficha.seccion("Demanda de citas por día de la semana").cabecera("Día", "Citas");
        List<String> etiquetasDia = new ArrayList<>();
        List<Number> valoresDia = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            ficha.fila(nombreDia(dia), String.valueOf(porDia[dia]));
            etiquetasDia.add(nombreDia(dia));
            valoresDia.add(porDia[dia]);
        }
        ficha.seccion("Uso de la agenda por médico").cabecera("Médico", "Citas", "Capacidad", "Utilización");
        HojaExcel agenda = HojaExcel.de("Agenda", "Médico", "Citas", "Capacidad", "Utilización %");
        for (ReporteOperativoResponse.AgendaMedico medico : r.agenda()) {
            ficha.fila(
                    medico.medico(),
                    String.valueOf(medico.citas()),
                    String.valueOf(medico.capacidad()),
                    Formato.porcentaje(medico.utilizacionPct()));
            agenda.fila(medico.medico(), medico.citas(), medico.capacidad(), medico.utilizacionPct());
        }
        ficha.seccion("Inventario crítico").cabecera("Producto", "Código", "Stock", "Mínimo");
        HojaExcel inventario = HojaExcel.de("Inventario", "Producto", "Código", "Stock", "Stock mínimo");
        for (ReporteOperativoResponse.ProductoCritico producto : r.inventario().bajoMinimo()) {
            ficha.fila(
                    producto.nombre(),
                    producto.codigo(),
                    String.valueOf(producto.stock()),
                    String.valueOf(producto.stockMinimo()));
            inventario.fila(producto.nombre(), producto.codigo(), producto.stock(), producto.stockMinimo());
        }
        ficha.filaTipo("T", "Lotes por vencer", String.valueOf(r.inventario().lotesPorVencer()))
                .filaTipo("T", "Lotes vencidos", String.valueOf(r.inventario().lotesVencidos()));
        if (graficos) {
            ficha.grafico(1, GraficosPdf.barras("Citas por día de la semana", etiquetasDia, valoresDia));
            if (!etiquetasArea.isEmpty()) {
                ficha.grafico(2, GraficosPdf.barras("Ocupación por área, en porcentaje", etiquetasArea, valoresArea));
            }
        }
        return new ReporteArmado(
                ficha,
                List.of(resumen, camas, esperas, emergencias, demanda, agenda, inventario),
                "Reporte operativo",
                nombre("operativo", r.periodo()));
    }

    private ReporteArmado calidad(ReporteCalidadResponse r, boolean graficos) {
        FichaDocumento ficha = base(TipoDocumento.REPORTE_CALIDAD, "Reporte de calidad", r.periodo(), null);
        HojaExcel resumen = HojaExcel.de("Resumen", "Concepto", "Valor");
        ficha.seccion("Satisfacción").cabecera("Concepto", "Valor");
        agregarResumen(
                ficha,
                resumen,
                "Valoración promedio",
                r.valoraciones().promedio(),
                Formato.numero(r.valoraciones().promedio(), 2));
        agregarResumen(
                ficha,
                resumen,
                "Valoraciones",
                r.valoraciones().total(),
                Formato.numero((double) r.valoraciones().total(), 0));
        agregarResumen(
                ficha,
                resumen,
                "Laboratorio completadas",
                r.laboratorio().completadas(),
                String.valueOf(r.laboratorio().completadas()));
        agregarResumen(
                ficha,
                resumen,
                "Entrega promedio de resultados en horas",
                r.laboratorio().entregaPromedioHoras(),
                Formato.numero(r.laboratorio().entregaPromedioHoras(), 1));
        agregarResumen(
                ficha,
                resumen,
                "Comprobantes anulados",
                r.anulaciones().comprobantes(),
                String.valueOf(r.anulaciones().comprobantes()));
        agregarResumen(
                ficha,
                resumen,
                "Dispensaciones anuladas",
                r.anulaciones().dispensaciones(),
                String.valueOf(r.anulaciones().dispensaciones()));
        HojaExcel medicos = tablaValoraciones(ficha, "Valoraciones por médico", r.porMedico(), "Médico");
        HojaExcel especialidades =
                tablaValoraciones(ficha, "Valoraciones por especialidad", r.porEspecialidad(), "Especialidad");
        ficha.seccion("Citas por especialidad")
                .cabecera("Especialidad", "Total", "Canceladas", "No asistieron", "Cancelación", "Ausencia");
        HojaExcel citas = HojaExcel.de(
                "Citas", "Especialidad", "Total", "Canceladas", "No asistieron", "Cancelación %", "Ausencia %");
        List<String> etiquetas = new ArrayList<>();
        List<Number> promedios = new ArrayList<>();
        for (ReporteCalidadResponse.ValoracionGrupo grupo : r.porEspecialidad()) {
            etiquetas.add(grupo.nombre());
            promedios.add(grupo.promedio());
        }
        for (ReporteCalidadResponse.Cancelacion cancelacion : r.citasPorEspecialidad()) {
            ficha.fila(
                    cancelacion.especialidad(),
                    String.valueOf(cancelacion.total()),
                    String.valueOf(cancelacion.canceladas()),
                    String.valueOf(cancelacion.noAsistieron()),
                    Formato.porcentaje(cancelacion.tasaCancelacionPct()),
                    Formato.porcentaje(cancelacion.tasaAusenciaPct()));
            citas.fila(
                    cancelacion.especialidad(),
                    cancelacion.total(),
                    cancelacion.canceladas(),
                    cancelacion.noAsistieron(),
                    cancelacion.tasaCancelacionPct(),
                    cancelacion.tasaAusenciaPct());
        }
        ficha.seccion("Laboratorio por prioridad").cabecera("Prioridad", "Cantidad", "Promedio en horas");
        HojaExcel laboratorio = HojaExcel.de("Laboratorio", "Prioridad", "Cantidad", "Promedio en horas");
        for (ReporteCalidadResponse.PorPrioridad prioridad : r.laboratorio().porPrioridad()) {
            ficha.fila(
                    prioridad.prioridad(),
                    String.valueOf(prioridad.cantidad()),
                    Formato.numero(prioridad.promedioHoras(), 1));
            laboratorio.fila(prioridad.prioridad(), prioridad.cantidad(), prioridad.promedioHoras());
        }
        HojaExcel anulaciones = HojaExcel.de("Anulaciones", "Documento", "Motivo", "Cantidad");
        ficha.seccion("Motivos de anulación").cabecera("Documento", "Motivo", "Cantidad");
        for (ReporteCalidadResponse.Motivo motivo : r.anulaciones().motivosComprobantes()) {
            ficha.fila("Comprobante", motivo.motivo(), String.valueOf(motivo.cantidad()));
            anulaciones.fila("Comprobante", motivo.motivo(), motivo.cantidad());
        }
        for (ReporteCalidadResponse.Motivo motivo : r.anulaciones().motivosDispensaciones()) {
            ficha.fila("Dispensación", motivo.motivo(), String.valueOf(motivo.cantidad()));
            anulaciones.fila("Dispensación", motivo.motivo(), motivo.cantidad());
        }
        if (graficos) {
            if (!etiquetas.isEmpty()) {
                ficha.grafico(1, GraficosPdf.barras("Valoración promedio por especialidad", etiquetas, promedios));
            }
            List<Long> distribucion = r.valoraciones().distribucion();
            if (distribucion != null && !distribucion.isEmpty()) {
                List<String> estrellas = new ArrayList<>();
                for (int i = 0; i < distribucion.size(); i++) {
                    estrellas.add((i + 1) + (i == 0 ? " estrella" : " estrellas"));
                }
                ficha.grafico(2, GraficosPdf.torta("Distribución de valoraciones", estrellas, distribucion));
            }
        }
        return new ReporteArmado(
                ficha,
                List.of(resumen, medicos, especialidades, citas, laboratorio, anulaciones),
                "Reporte de calidad",
                nombre("calidad", r.periodo()));
    }

    private FichaDocumento base(TipoDocumento tipo, String titulo, PeriodoReporte periodo, PeriodoReporte anterior) {
        FichaDocumento ficha = FichaDocumento.de(tipo)
                .titulo(titulo, "Del " + Formato.fecha(periodo.desde()) + " al " + Formato.fecha(periodo.hasta()))
                .par("Periodo", Formato.fecha(periodo.desde()) + " al " + Formato.fecha(periodo.hasta()))
                .par("Días", String.valueOf(periodo.dias()))
                .par("Agrupación", unidad(periodo.agrupacion()));
        if (anterior != null) {
            ficha.par("Periodo anterior", Formato.fecha(anterior.desde()) + " al " + Formato.fecha(anterior.hasta()));
        }
        return ficha;
    }

    private HojaExcel tablaItems(
            FichaDocumento ficha, String titulo, List<ReporteFinancieroResponse.Item> items, String columna) {
        ficha.seccion(titulo).cabecera(columna, "Cantidad", "Total");
        HojaExcel hoja = HojaExcel.de(titulo, columna, "Cantidad", "Total");
        for (ReporteFinancieroResponse.Item item : items) {
            ficha.fila(item.nombre(), String.valueOf(item.cantidad()), Formato.moneda(item.total()));
            hoja.fila(item.nombre(), item.cantidad(), item.total());
        }
        return hoja;
    }

    private HojaExcel tablaValoraciones(
            FichaDocumento ficha, String titulo, List<ReporteCalidadResponse.ValoracionGrupo> grupos, String columna) {
        ficha.seccion(titulo).cabecera(columna, "Valoraciones", "Promedio");
        HojaExcel hoja = HojaExcel.de(titulo, columna, "Valoraciones", "Promedio", "1", "2", "3", "4", "5");
        for (ReporteCalidadResponse.ValoracionGrupo grupo : grupos) {
            ficha.fila(grupo.nombre(), String.valueOf(grupo.total()), Formato.numero(grupo.promedio(), 2));
            List<Object> fila = new ArrayList<>(List.of(grupo.nombre(), grupo.total(), grupo.promedio()));
            for (int i = 0; i < 5; i++) {
                fila.add(
                        grupo.distribucion() != null && i < grupo.distribucion().size()
                                ? grupo.distribucion().get(i)
                                : 0L);
            }
            hoja.fila(fila.toArray());
        }
        return hoja;
    }

    private void agregarResumen(FichaDocumento ficha, HojaExcel hoja, String concepto, Object valor, String texto) {
        ficha.fila(concepto, texto);
        hoja.fila(concepto, valor);
    }

    private static Object[] linea(String nombre, IndicadorReporte indicador, Function<BigDecimal, String> formato) {
        return new Object[] {
            nombre,
            indicador,
            indicador.actual() == null ? SIN_DATO : formato.apply(indicador.actual()),
            indicador.anterior() == null ? SIN_DATO : formato.apply(indicador.anterior())
        };
    }

    private static String variacion(IndicadorReporte indicador) {
        if (indicador.variacionPct() == null) {
            return SIN_DATO;
        }
        String signo = indicador.variacionPct().signum() > 0 ? "+" : "";
        return signo + Formato.numero(indicador.variacionPct(), 1) + " %";
    }

    private static String unidad(AgrupacionReporte agrupacion) {
        if (agrupacion == null) {
            return "día";
        }
        return switch (agrupacion) {
            case DIA -> "día";
            case SEMANA -> "semana";
            case MES -> "mes";
        };
    }

    private static String corta(LocalDate fecha) {
        return String.format("%02d/%02d", fecha.getDayOfMonth(), fecha.getMonthValue());
    }

    private static String nombreDia(int dia) {
        return java.time.DayOfWeek.of(dia).getDisplayName(TextStyle.FULL, Locale.of("es", "PE"));
    }

    private static String nombre(String tipo, PeriodoReporte periodo) {
        return "reporte-" + tipo + "-" + periodo.desde() + "-a-" + periodo.hasta();
    }
}
