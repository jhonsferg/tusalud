package com.codesoftlabs.tusalud.app.canales;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RenderizadorMensaje {

    public static final Set<String> VARIABLES = Set.of("nombre", "enlace", "centro");

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z_]+)\\s*}}");
    private static final Pattern URL = Pattern.compile("https?://[^\\s<]+");
    private static final String COLOR_MARCA = "#0f766e";

    private RenderizadorMensaje() {}

    public static List<String> variablesDesconocidas(String plantilla) {
        List<String> desconocidas = new ArrayList<>();
        Matcher m = VARIABLE.matcher(plantilla == null ? "" : plantilla);
        while (m.find()) {
            String nombre = m.group(1);
            if (!VARIABLES.contains(nombre) && !desconocidas.contains(nombre)) {
                desconocidas.add(nombre);
            }
        }
        return desconocidas;
    }

    public static String renderizar(String plantilla, Map<String, String> valores) {
        Matcher m = VARIABLE.matcher(plantilla);
        StringBuilder salida = new StringBuilder();
        while (m.find()) {
            String valor = valores.getOrDefault(m.group(1), "");
            m.appendReplacement(salida, Matcher.quoteReplacement(valor));
        }
        m.appendTail(salida);
        return salida.toString();
    }

    public static String textoCorreo(String cuerpo, String enlaceBaja) {
        return cuerpo + "\n\n--\nPara dejar de recibir estos correos: " + enlaceBaja
                + "\nEste mensaje no contiene información clínica.";
    }

    public static String textoMensajeria(String cuerpo, String enlaceBaja) {
        return cuerpo + " Baja: " + enlaceBaja;
    }

    public static String html(String asunto, String cuerpo, String centro, String enlaceBaja) {
        StringBuilder parrafos = new StringBuilder();
        for (String parrafo : cuerpo.split("\\n\\s*\\n")) {
            parrafos.append("<p style=\"margin:0 0 16px;font-size:15px;line-height:1.6;color:#1f2937\">")
                    .append(enlazar(escapar(parrafo.strip()).replace("\n", "<br>")))
                    .append("</p>");
        }
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\""
                + " content=\"width=device-width,initial-scale=1\"><title>" + escapar(asunto) + "</title></head>"
                + "<body style=\"margin:0;padding:24px;background:#f3f4f6;font-family:Segoe UI,Arial,sans-serif\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"560\" cellspacing=\"0\" cellpadding=\"0\""
                + " style=\"max-width:560px;background:#ffffff;border-radius:8px;overflow:hidden\">"
                + "<tr><td style=\"background:" + COLOR_MARCA + ";padding:20px 28px;color:#ffffff;font-size:20px;"
                + "font-weight:600\">" + escapar(centro) + "</td></tr>"
                + "<tr><td style=\"padding:28px\"><h1 style=\"margin:0 0 16px;font-size:18px;color:#111827\">"
                + escapar(asunto) + "</h1>" + parrafos + "</td></tr>"
                + "<tr><td style=\"padding:16px 28px;background:#f9fafb;font-size:12px;line-height:1.5;color:#6b7280\">"
                + "Este mensaje no contiene información clínica. Para consultar su información ingrese a su cuenta."
                + "<br><a href=\"" + escapar(enlaceBaja) + "\" style=\"color:" + COLOR_MARCA
                + "\">Dejar de recibir estos correos</a></td></tr></table></td></tr></table></body></html>";
    }

    private static String enlazar(String textoEscapado) {
        Matcher m = URL.matcher(textoEscapado);
        StringBuilder salida = new StringBuilder();
        while (m.find()) {
            String url = m.group();
            m.appendReplacement(
                    salida,
                    Matcher.quoteReplacement(
                            "<a href=\"" + url + "\" style=\"color:" + COLOR_MARCA + "\">" + url + "</a>"));
        }
        m.appendTail(salida);
        return salida.toString();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
