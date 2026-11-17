package com.codesoftlabs.tusalud.app.security;

public final class DispositivoParser {

    private static final String DESCONOCIDO = "Dispositivo desconocido";

    private DispositivoParser() {}

    public static String describir(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DESCONOCIDO;
        }
        return navegador(userAgent) + " en " + sistema(userAgent);
    }

    private static String navegador(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("Edge/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Chrome/") && !ua.contains("Chromium")) {
            return "Chrome";
        }
        if (ua.contains("Firefox/")) {
            return "Firefox";
        }
        if (ua.contains("Safari/")) {
            return "Safari";
        }
        return "Navegador desconocido";
    }

    private static String sistema(String ua) {
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("iPhone") || ua.contains("iPad")) {
            return "iOS";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "sistema desconocido";
    }
}
