package com.codesoftlabs.tusalud.app.services.odontologia;

public final class Dentadura {

    public static final String PERMANENTE = "PERMANENTE";
    public static final String TEMPORAL = "TEMPORAL";

    private Dentadura() {}

    public static boolean valida(int pieza) {
        int cuadrante = pieza / 10;
        int posicion = pieza % 10;
        if (cuadrante >= 1 && cuadrante <= 4) {
            return posicion >= 1 && posicion <= 8;
        }
        if (cuadrante >= 5 && cuadrante <= 8) {
            return posicion >= 1 && posicion <= 5;
        }
        return false;
    }

    public static String tipo(int pieza) {
        return pieza / 10 <= 4 ? PERMANENTE : TEMPORAL;
    }
}
