package com.codesoftlabs.tusalud.app.documentos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Formato() {}

    public static String moneda(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        return "S/ " + numero(valor, 2);
    }

    public static String numero(BigDecimal valor, int decimales) {
        if (valor == null) {
            return "";
        }
        DecimalFormat formato = new DecimalFormat(
                decimales == 0 ? "#,##0" : "#,##0." + "0".repeat(decimales),
                DecimalFormatSymbols.getInstance(Locale.US));
        return formato.format(valor.setScale(decimales, RoundingMode.HALF_UP));
    }

    public static String numero(double valor, int decimales) {
        return numero(BigDecimal.valueOf(valor), decimales);
    }

    public static String porcentaje(BigDecimal valor) {
        return valor == null ? "" : numero(valor, 1) + " %";
    }

    public static String fecha(LocalDate valor) {
        return valor == null ? "" : FECHA.format(valor);
    }

    public static String fechaHora(LocalDateTime valor) {
        return valor == null ? "" : FECHA_HORA.format(valor);
    }

    public static String hora(java.time.LocalTime valor) {
        return valor == null ? "" : HORA.format(valor);
    }

    public static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    public static String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace('\r', ' ').replace('\u0000', ' ').strip();
    }
}
