package com.codesoftlabs.tusalud.app.entities.enums;

public enum ClasificacionTriaje {
    ROJO,
    NARANJA,
    AMARILLO,
    VERDE,
    AZUL;

    public static ClasificacionTriaje deNivel(int nivel) {
        return switch (nivel) {
            case 1 -> ROJO;
            case 2 -> NARANJA;
            case 3 -> AMARILLO;
            case 4 -> VERDE;
            case 5 -> AZUL;
            default -> throw new IllegalArgumentException("Nivel de triaje fuera de rango: " + nivel);
        };
    }
}
