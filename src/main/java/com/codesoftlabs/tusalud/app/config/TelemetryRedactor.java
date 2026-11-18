package com.codesoftlabs.tusalud.app.config;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class TelemetryRedactor {

    public static final String REDACTADO = "[REDACTADO]";
    public static final String TRUNCADO = "...[TRUNCADO]";
    public static final int MAX_CUERPO = 4096;
    static final int MAX_ANALIZABLE = 256 * 1024;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> HEADERS_SENSIBLES = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token",
            "x-csrf-token",
            "x-xsrf-token",
            "x-access-token",
            "x-refresh-token");

    private static final Set<String> CAMPOS_EXACTOS = Set.of(
            "codigo",
            "codigomfa",
            "codigorecuperacion",
            "codigosrecuperacion",
            "clave",
            "numerotarjeta",
            "numero",
            "pan",
            "qr",
            "cvc",
            "otp",
            "totp");

    private static final List<String> FRAGMENTOS_SENSIBLES =
            List.of("pass", "secret", "secreto", "token", "cvv", "cvc", "tarjeta", "recuperacion");

    private static final Pattern RUTAS_SIN_CUERPO = Pattern.compile("^/api/v1/(" + "auth(/.*)?" + "|perfil(/.*)?"
            + "|metodos-pago(/.*)?"
            + "|pagos(/.*)?"
            + "|admin/(usuarios|personal|roles|pagos)(/.*)?"
            + "|recepcion/pacientes(/.*)?"
            + "|historia-clinica(/.*)?"
            + "|pacientes/[^/]+/(historia-clinica|datos-medicos)(/.*)?"
            + "|laboratorio(/.*)?"
            + "|imagenes(/.*)?"
            + "|archivos(/.*)?"
            + "|documentos(/.*)?"
            + "|exportaciones(/.*)?"
            + "|plantillas(/.*)?"
            + "|hospitalizacion(/.*)?"
            + "|farmacia(/.*)?"
            + "|citas/[^/]+/(atencion|pagos?)(/.*)?"
            + ")/?$");

    private TelemetryRedactor() {}

    public static boolean esHeaderSensible(String nombre) {
        return HEADERS_SENSIBLES.contains(nombre.toLowerCase(Locale.ROOT));
    }

    public static List<String> valoresHeader(String nombre, List<String> valores) {
        if (!esHeaderSensible(nombre)) {
            return valores;
        }
        return valores.stream().map(v -> REDACTADO).toList();
    }

    public static boolean registraCuerpo(String ruta) {
        return ruta != null && !RUTAS_SIN_CUERPO.matcher(ruta).matches();
    }

    public static boolean esJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            MediaType tipo = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.isCompatibleWith(tipo)
                    || "json".equalsIgnoreCase(tipo.getSubtype())
                    || tipo.getSubtype().toLowerCase(Locale.ROOT).endsWith("+json");
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    public static String cuerpoSeguro(String ruta, String contentType, byte[] cuerpo, Charset charset) {
        if (!registraCuerpo(ruta) || cuerpo == null || cuerpo.length == 0 || !esJson(contentType)) {
            return null;
        }
        if (cuerpo.length > MAX_ANALIZABLE) {
            return null;
        }
        JsonNode nodo;
        try {
            nodo = MAPPER.readTree(new String(cuerpo, charset));
        } catch (RuntimeException e) {
            return null;
        }
        redactar(nodo);
        String texto = nodo.toString();
        if (texto.length() > MAX_CUERPO) {
            return texto.substring(0, MAX_CUERPO) + TRUNCADO;
        }
        return texto;
    }

    static boolean esCampoSensible(String nombre) {
        String clave = nombre.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        if (CAMPOS_EXACTOS.contains(clave)) {
            return true;
        }
        for (String fragmento : FRAGMENTOS_SENSIBLES) {
            if (clave.contains(fragmento)) {
                return true;
            }
        }
        return false;
    }

    private static void redactar(JsonNode nodo) {
        if (nodo instanceof ObjectNode objeto) {
            for (String nombre : List.copyOf(objeto.propertyNames())) {
                JsonNode valor = objeto.get(nombre);
                if (esCampoSensible(nombre)) {
                    objeto.put(nombre, REDACTADO);
                } else {
                    redactar(valor);
                }
            }
        } else if (nodo instanceof ArrayNode arreglo) {
            for (JsonNode elemento : arreglo) {
                redactar(elemento);
            }
        }
    }
}
