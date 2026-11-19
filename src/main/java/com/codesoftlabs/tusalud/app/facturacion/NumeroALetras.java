package com.codesoftlabs.tusalud.app.facturacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumeroALetras {

    private static final String[] UNIDADES = {
        "",
        "UNO",
        "DOS",
        "TRES",
        "CUATRO",
        "CINCO",
        "SEIS",
        "SIETE",
        "OCHO",
        "NUEVE",
        "DIEZ",
        "ONCE",
        "DOCE",
        "TRECE",
        "CATORCE",
        "QUINCE",
        "DIECISEIS",
        "DIECISIETE",
        "DIECIOCHO",
        "DIECINUEVE",
        "VEINTE",
        "VEINTIUNO",
        "VEINTIDOS",
        "VEINTITRES",
        "VEINTICUATRO",
        "VEINTICINCO",
        "VEINTISEIS",
        "VEINTISIETE",
        "VEINTIOCHO",
        "VEINTINUEVE"
    };
    private static final String[] DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "",
        "CIENTO",
        "DOSCIENTOS",
        "TRESCIENTOS",
        "CUATROCIENTOS",
        "QUINIENTOS",
        "SEISCIENTOS",
        "SETECIENTOS",
        "OCHOCIENTOS",
        "NOVECIENTOS"
    };

    private NumeroALetras() {}

    public static String soles(BigDecimal monto) {
        BigDecimal redondeado = monto.setScale(2, RoundingMode.HALF_UP);
        long enteros = redondeado.toBigInteger().longValueExact();
        int centimos =
                redondeado.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String texto = enteros == 0 ? "CERO" : convertir(enteros);
        return String.format("%s CON %02d/100 SOLES", texto, centimos);
    }

    private static String convertir(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            String cabeza = millones == 1 ? "UN MILLON" : convertir(millones) + " MILLONES";
            long resto = n % 1_000_000;
            return resto == 0 ? cabeza : cabeza + " " + convertir(resto);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            String cabeza = miles == 1 ? "MIL" : convertir(miles).replaceAll("UNO$", "UN") + " MIL";
            long resto = n % 1000;
            return resto == 0 ? cabeza : cabeza + " " + convertir(resto);
        }
        return menorDeMil((int) n);
    }

    private static String menorDeMil(int n) {
        if (n == 100) {
            return "CIEN";
        }
        StringBuilder texto = new StringBuilder();
        if (n >= 100) {
            texto.append(CENTENAS[n / 100]);
            n %= 100;
            if (n > 0) {
                texto.append(' ');
            }
        }
        if (n > 0) {
            if (n < 30) {
                texto.append(UNIDADES[n]);
            } else {
                texto.append(DECENAS[n / 10]);
                if (n % 10 > 0) {
                    texto.append(" Y ").append(UNIDADES[n % 10]);
                }
            }
        }
        return texto.toString();
    }
}
