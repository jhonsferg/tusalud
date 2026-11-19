package com.codesoftlabs.tusalud.app.services.cie10;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Cie10Normalizador {

    public static final Pattern FORMATO_CODIGO = Pattern.compile("^[A-Z][0-9]{2}(\\.[0-9A-Z]{1,4})?$");

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern COMODINES = Pattern.compile("[%_\\\\]");

    private Cie10Normalizador() {}

    public static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        String sinMarcas = MARCAS.matcher(Normalizer.normalize(valor.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
                .replaceAll("");
        return ESPACIOS.matcher(sinMarcas.trim()).replaceAll(" ");
    }

    public static String codigo(String valor) {
        return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    public static String busqueda(String codigo, String descripcion) {
        return texto(descripcion) + " " + codigo.toLowerCase(Locale.ROOT);
    }

    public static String sinComodines(String valor) {
        return COMODINES.matcher(valor).replaceAll("");
    }
}
