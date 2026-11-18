package com.codesoftlabs.tusalud.app.storage;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class NombresArchivo {

    private static final int MAXIMO = 120;
    private static final Pattern NO_PERMITIDOS = Pattern.compile("[^\\p{L}\\p{N}._ \\-]");
    private static final Pattern PUNTOS = Pattern.compile("\\.{2,}");

    private NombresArchivo() {}

    public static String sanitizar(String original) {
        String nombre = original == null ? "" : original;
        int barra = Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\'));
        if (barra >= 0) {
            nombre = nombre.substring(barra + 1);
        }
        nombre = Normalizer.normalize(nombre, Normalizer.Form.NFKC);
        nombre = NO_PERMITIDOS.matcher(nombre).replaceAll("_");
        nombre = PUNTOS.matcher(nombre).replaceAll(".");
        nombre = nombre.strip();
        while (nombre.startsWith(".") || nombre.startsWith("-")) {
            nombre = nombre.substring(1);
        }
        if (nombre.isBlank()) {
            return "archivo";
        }
        if (nombre.length() > MAXIMO) {
            String extension = extension(nombre);
            int disponible = MAXIMO - (extension.isEmpty() ? 0 : extension.length() + 1);
            String base = nombre.substring(0, Math.max(1, disponible));
            nombre = extension.isEmpty() ? base : base + "." + extension;
        }
        return nombre;
    }

    public static String extension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return "";
        }
        return nombre.substring(punto + 1).toLowerCase(java.util.Locale.ROOT);
    }
}
