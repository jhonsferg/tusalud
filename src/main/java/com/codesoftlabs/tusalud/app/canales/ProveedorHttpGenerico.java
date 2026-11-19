package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.config.CanalesProperties.Http;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class ProveedorHttpGenerico implements ProveedorMensajeria {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Http configuracion;
    private final RestClient cliente;

    public ProveedorHttpGenerico(Http configuracion) {
        this.configuracion = configuracion;
        int segundos = configuracion.tiempoLimiteSeg() == null ? 10 : configuracion.tiempoLimiteSeg();
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(segundos))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(segundos));
        this.cliente = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public String enviar(MensajeSaliente mensaje) {
        if (vacio(configuracion.url()) || vacio(configuracion.cuerpo())) {
            throw new EnvioException("El proveedor no está configurado", false);
        }
        boolean json = "JSON".equalsIgnoreCase(configuracion.formato());
        String cuerpo = configuracion
                .cuerpo()
                .replace("{destino}", codificar(json, mensaje.destino()))
                .replace("{texto}", codificar(json, mensaje.texto()));
        try {
            String respuesta = cliente.post()
                    .uri(configuracion.url())
                    .contentType(json ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(this::autenticar)
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);
            return extraerId(respuesta, mensaje);
        } catch (HttpStatusCodeException ex) {
            int estado = ex.getStatusCode().value();
            boolean transitorio = estado == 408 || estado == 429 || estado >= 500;
            throw new EnvioException("El proveedor respondió con estado " + estado, !transitorio, ex);
        } catch (ResourceAccessException ex) {
            throw new EnvioException("No se pudo contactar al proveedor", false, ex);
        }
    }

    private void autenticar(HttpHeaders cabeceras) {
        String modo = configuracion.autenticacion() == null ? "NONE" : configuracion.autenticacion();
        if ("BEARER".equalsIgnoreCase(modo)) {
            cabeceras.setBearerAuth(configuracion.secreto());
        } else if ("BASIC".equalsIgnoreCase(modo)) {
            String credencial = configuracion.usuario() + ":" + configuracion.secreto();
            cabeceras.set(
                    HttpHeaders.AUTHORIZATION,
                    "Basic " + Base64.getEncoder().encodeToString(credencial.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private String extraerId(String respuesta, MensajeSaliente mensaje) {
        String respaldo = "http-" + mensaje.claveIdempotencia();
        if (vacio(respuesta) || vacio(configuracion.campoIdExterno())) {
            return respaldo;
        }
        try {
            JsonNode nodo = MAPPER.readTree(respuesta);
            for (String parte : configuracion.campoIdExterno().split("\\.")) {
                nodo = parte.chars().allMatch(Character::isDigit)
                        ? nodo.path(Integer.parseInt(parte))
                        : nodo.path(parte);
            }
            String id = nodo.asString("");
            return id.isBlank() ? respaldo : id;
        } catch (RuntimeException ex) {
            return respaldo;
        }
    }

    private static String codificar(boolean json, String valor) {
        if (json) {
            String texto = MAPPER.writeValueAsString(valor);
            return texto.substring(1, texto.length() - 1);
        }
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
