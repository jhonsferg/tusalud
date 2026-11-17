package com.codesoftlabs.tusalud.app.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public record ContextoConexion(String ip, String userAgent) {

    private static final int MAX_IP = 45;
    private static final int MAX_USER_AGENT = 500;
    private static final String CABECERA_FORWARDED_FOR = "X-Forwarded-For";

    public static ContextoConexion desde(HttpServletRequest request) {
        String forwarded = request.getHeader(CABECERA_FORWARDED_FOR);
        String ip = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return new ContextoConexion(
                recortar(ip, MAX_IP), recortar(request.getHeader(HttpHeaders.USER_AGENT), MAX_USER_AGENT));
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }
}
