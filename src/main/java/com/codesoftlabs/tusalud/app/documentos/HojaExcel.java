package com.codesoftlabs.tusalud.app.documentos;

import java.util.ArrayList;
import java.util.List;

public final class HojaExcel {

    private final String nombre;
    private final List<String> columnas;
    private final List<List<Object>> filas = new ArrayList<>();

    public HojaExcel(String nombre, List<String> columnas) {
        this.nombre = nombre;
        this.columnas = columnas;
    }

    public static HojaExcel de(String nombre, String... columnas) {
        return new HojaExcel(nombre, List.of(columnas));
    }

    public HojaExcel fila(Object... valores) {
        filas.add(new ArrayList<>(java.util.Arrays.asList(valores)));
        return this;
    }

    public String nombre() {
        return nombre;
    }

    public List<String> columnas() {
        return columnas;
    }

    public List<List<Object>> filas() {
        return filas;
    }
}
