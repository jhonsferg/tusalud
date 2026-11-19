package com.codesoftlabs.tusalud.app.facturacion;

public final class ValidadorDocumentos {

    private static final int[] PESOS_RUC = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private ValidadorDocumentos() {}

    public static boolean rucValido(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}")) {
            return false;
        }
        String prefijo = ruc.substring(0, 2);
        if (!prefijo.equals("10") && !prefijo.equals("15") && !prefijo.equals("17") && !prefijo.equals("20")) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * PESOS_RUC[i];
        }
        int digito = 11 - (suma % 11);
        if (digito == 10) {
            digito = 0;
        } else if (digito == 11) {
            digito = 1;
        }
        return digito == ruc.charAt(10) - '0';
    }

    public static boolean dniValido(String dni) {
        return dni != null && dni.matches("\\d{8}") && !dni.equals("00000000");
    }
}
