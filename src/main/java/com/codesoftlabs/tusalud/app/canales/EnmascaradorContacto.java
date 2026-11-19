package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;

public final class EnmascaradorContacto {

    private EnmascaradorContacto() {}

    public static String enmascarar(Canal canal, String destino) {
        if (destino == null || destino.isBlank()) {
            return "";
        }
        return canal == Canal.CORREO ? correo(destino) : telefono(destino);
    }

    private static String correo(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        String local = correo.substring(0, arroba);
        String dominio = correo.substring(arroba);
        return local.charAt(0) + "***" + dominio;
    }

    private static String telefono(String telefono) {
        String digitos = telefono.replaceAll("\\D", "");
        if (digitos.length() < 4) {
            return "***";
        }
        return "***" + digitos.substring(digitos.length() - 3);
    }
}
