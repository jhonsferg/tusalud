package com.codesoftlabs.tusalud.app.documentos;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public final class GraficosPdf {

    private static final int ANCHO = 1000;
    private static final int ALTO = 600;
    private static final Color AZUL = new Color(0x1D, 0x4E, 0xD8);
    private static final Color[] PALETA = {
        new Color(0x1D, 0x4E, 0xD8),
        new Color(0x10, 0xB9, 0x81),
        new Color(0xF5, 0x9E, 0x0B),
        new Color(0x8B, 0x5C, 0xF6),
        new Color(0xEF, 0x44, 0x44),
        new Color(0x64, 0x74, 0x8B)
    };
    private static final int MAXIMO_CATEGORIAS = 31;

    private GraficosPdf() {}

    public static BufferedImage barras(String titulo, List<String> etiquetas, List<? extends Number> valores) {
        DefaultCategoryDataset datos = dataset(etiquetas, valores);
        JFreeChart grafico =
                ChartFactory.createBarChart(titulo, null, null, datos, PlotOrientation.VERTICAL, false, false, false);
        estilo(grafico);
        CategoryPlot plot = grafico.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, AZUL);
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.12);
        return grafico.createBufferedImage(ANCHO, ALTO);
    }

    public static BufferedImage lineas(String titulo, List<String> etiquetas, List<? extends Number> valores) {
        DefaultCategoryDataset datos = dataset(etiquetas, valores);
        JFreeChart grafico =
                ChartFactory.createLineChart(titulo, null, null, datos, PlotOrientation.VERTICAL, false, false, false);
        estilo(grafico);
        CategoryPlot plot = grafico.getCategoryPlot();
        LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, AZUL);
        renderer.setSeriesStroke(0, new java.awt.BasicStroke(2.5f));
        plot.setRenderer(renderer);
        return grafico.createBufferedImage(ANCHO, ALTO);
    }

    public static BufferedImage torta(String titulo, List<String> etiquetas, List<? extends Number> valores) {
        DefaultPieDataset<String> datos = new DefaultPieDataset<>();
        for (int i = 0; i < etiquetas.size() && i < valores.size(); i++) {
            double valor = valores.get(i).doubleValue();
            if (valor > 0) {
                datos.setValue(etiquetas.get(i), valor);
            }
        }
        JFreeChart grafico = ChartFactory.createPieChart(titulo, datos, true, false, false);
        grafico.setBackgroundPaint(Color.WHITE);
        grafico.getTitle().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        PiePlot plot = (PiePlot) grafico.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 26));
        plot.setLabelGenerator(null);
        for (int i = 0; i < etiquetas.size(); i++) {
            plot.setSectionPaint(etiquetas.get(i), PALETA[i % PALETA.length]);
        }
        grafico.getLegend().setItemFont(new Font(Font.SANS_SERIF, Font.PLAIN, 26));
        return grafico.createBufferedImage(ANCHO, ALTO);
    }

    private static DefaultCategoryDataset dataset(List<String> etiquetas, List<? extends Number> valores) {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        int total = Math.min(Math.min(etiquetas.size(), valores.size()), MAXIMO_CATEGORIAS);
        for (int i = 0; i < total; i++) {
            datos.addValue(valores.get(i), "Serie", etiquetas.get(i));
        }
        return datos;
    }

    private static void estilo(JFreeChart grafico) {
        grafico.setBackgroundPaint(Color.WHITE);
        grafico.getTitle().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        CategoryPlot plot = grafico.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(0xCB, 0xD5, 0xE1));
        plot.setOutlineVisible(false);
        plot.getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        plot.getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        plot.getDomainAxis().setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);
    }
}
