package com.codesoftlabs.tusalud.app.documentos;

import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FichaDocumento {

    private static final int PARES_MAXIMOS = 8;
    private static final int TOTALES_MAXIMOS = 3;

    private final String plantilla;
    private final Map<String, Object> parametros = new HashMap<>();
    private final List<Map<String, Object>> filas = new ArrayList<>();
    private int pares;
    private int totales;

    private FichaDocumento(String plantilla) {
        this.plantilla = plantilla;
    }

    public static FichaDocumento de(TipoDocumento tipo) {
        return new FichaDocumento(tipo.plantilla());
    }

    public FichaDocumento titulo(String titulo, String subtitulo) {
        parametros.put("titulo", Formato.limpiar(titulo));
        parametros.put("subtitulo", Formato.limpiar(subtitulo));
        return this;
    }

    public FichaDocumento banner(String texto) {
        parametros.put("banner", Formato.limpiar(texto));
        return this;
    }

    public FichaDocumento par(String etiqueta, String valor) {
        if (pares >= PARES_MAXIMOS) {
            throw new IllegalStateException("La ficha admite como máximo " + PARES_MAXIMOS + " datos");
        }
        pares++;
        parametros.put("l" + pares, Formato.limpiar(etiqueta));
        parametros.put("v" + pares, Formato.limpiar(valor));
        return this;
    }

    public FichaDocumento total(String etiqueta, String valor) {
        if (totales >= TOTALES_MAXIMOS) {
            throw new IllegalStateException("La ficha admite como máximo " + TOTALES_MAXIMOS + " totales");
        }
        totales++;
        parametros.put("tl" + totales, Formato.limpiar(etiqueta));
        parametros.put("tv" + totales, Formato.limpiar(valor));
        return this;
    }

    public FichaDocumento nota(String titulo, String texto) {
        parametros.put("notaTitulo", Formato.limpiar(titulo));
        parametros.put("nota", Formato.limpiar(texto));
        return this;
    }

    public FichaDocumento firma(String linea1, String linea2) {
        parametros.put("firma1", Formato.limpiar(linea1));
        parametros.put("firma2", Formato.limpiar(linea2));
        return this;
    }

    public FichaDocumento grafico(int posicion, Image imagen) {
        if (posicion < 1 || posicion > 2) {
            throw new IllegalArgumentException("Solo hay dos espacios para gráficos");
        }
        if (imagen != null) {
            parametros.put("g" + posicion, imagen);
        }
        return this;
    }

    public FichaDocumento fila(String... columnas) {
        return filaTipada("D", false, columnas);
    }

    public FichaDocumento filaFuera(boolean fuera, String... columnas) {
        return filaTipada("D", fuera, columnas);
    }

    public FichaDocumento seccion(String titulo) {
        return filaTipada("S", false, titulo);
    }

    public FichaDocumento cabecera(String... columnas) {
        return filaTipada("H", false, columnas);
    }

    public FichaDocumento filaTipo(String tipo, String... columnas) {
        return filaTipada(tipo, false, columnas);
    }

    private FichaDocumento filaTipada(String tipo, boolean fuera, String... columnas) {
        Map<String, Object> fila = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            fila.put("c" + (i + 1), i < columnas.length ? Formato.limpiar(columnas[i]) : "");
        }
        fila.put("tipo", tipo);
        fila.put("fuera", fuera ? "1" : "0");
        filas.add(fila);
        return this;
    }

    public String plantilla() {
        return plantilla;
    }

    public Map<String, Object> parametros() {
        return parametros;
    }

    public List<Map<String, Object>> filas() {
        return filas;
    }

    public int cantidadFilas() {
        return filas.size();
    }
}
