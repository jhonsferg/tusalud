package com.codesoftlabs.tusalud.app.services.receta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CsvSimple {

    private CsvSimple() {}

    static List<List<String>> leer(String texto) {
        char separador = separador(texto);
        List<List<String>> registros = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                        celda.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    celda.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == separador) {
                actual.add(celda.toString());
                celda.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                actual.add(celda.toString());
                celda.setLength(0);
                registros.add(actual);
                actual = new ArrayList<>();
            } else {
                celda.append(c);
            }
        }
        if (celda.length() > 0 || !actual.isEmpty()) {
            actual.add(celda.toString());
            registros.add(actual);
        }
        return registros;
    }

    static Map<String, Integer> columnas(List<String> cabecera) {
        Map<String, Integer> mapa = new HashMap<>();
        for (int i = 0; i < cabecera.size(); i++) {
            mapa.put(cabecera.get(i).strip().toLowerCase(Locale.ROOT), i);
        }
        return mapa;
    }

    static String valor(List<String> celdas, Map<String, Integer> columnas, String nombre) {
        Integer indice = columnas.get(nombre);
        if (indice == null || indice >= celdas.size()) {
            return "";
        }
        return celdas.get(indice).strip();
    }

    private static char separador(String texto) {
        int fin = texto.indexOf('\n');
        String linea = fin < 0 ? texto : texto.substring(0, fin);
        long comas = linea.chars().filter(c -> c == ',').count();
        long puntoYComa = linea.chars().filter(c -> c == ';').count();
        long tabuladores = linea.chars().filter(c -> c == '\t').count();
        if (puntoYComa >= comas && puntoYComa >= tabuladores && puntoYComa > 0) {
            return ';';
        }
        if (tabuladores > comas) {
            return '\t';
        }
        return ',';
    }
}
