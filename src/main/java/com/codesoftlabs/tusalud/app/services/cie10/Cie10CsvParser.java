package com.codesoftlabs.tusalud.app.services.cie10;

import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Cie10CsvParser {

    public static final int MAXIMO_FILAS = 30_000;
    public static final String CABECERA = "codigo,descripcion,capitulo,grupo,sexo,edad_minima,edad_maxima";

    public record Fila(
            int numero,
            String codigo,
            String descripcion,
            String capitulo,
            String grupo,
            SexoPaciente sexo,
            Integer edadMinima,
            Integer edadMaxima) {}

    public record Error(int fila, String mensaje) {}

    public record Resultado(int filas, List<Fila> validas, List<Error> errores) {}

    private Cie10CsvParser() {}

    public static Resultado parsear(byte[] contenido) {
        String texto = new String(contenido, StandardCharsets.UTF_8);
        if (!texto.isEmpty() && texto.charAt(0) == '\uFEFF') {
            texto = texto.substring(1);
        }
        List<List<String>> registros = leer(texto);
        List<Error> errores = new ArrayList<>();
        List<Fila> validas = new ArrayList<>();
        if (registros.isEmpty()) {
            errores.add(new Error(0, "El archivo está vacío"));
            return new Resultado(0, validas, errores);
        }
        Map<String, Integer> columnas = columnas(registros.get(0), errores);
        if (!errores.isEmpty()) {
            return new Resultado(0, validas, errores);
        }
        int total = registros.size() - 1;
        if (total > MAXIMO_FILAS) {
            errores.add(new Error(0, "El archivo supera el máximo de " + MAXIMO_FILAS + " filas"));
            return new Resultado(total, validas, errores);
        }
        Set<String> vistos = new HashSet<>();
        for (int i = 1; i < registros.size(); i++) {
            List<String> celdas = registros.get(i);
            int numero = i + 1;
            if (celdas.stream().allMatch(String::isBlank)) {
                total--;
                continue;
            }
            String codigo = Cie10Normalizador.codigo(valor(celdas, columnas, "codigo"));
            String descripcion = valor(celdas, columnas, "descripcion").trim();
            String capitulo = valor(celdas, columnas, "capitulo").trim();
            String grupo = valor(celdas, columnas, "grupo").trim();
            String sexoTexto = valor(celdas, columnas, "sexo").trim();
            String minimoTexto = valor(celdas, columnas, "edad_minima").trim();
            String maximoTexto = valor(celdas, columnas, "edad_maxima").trim();
            String problema = null;
            SexoPaciente sexo = null;
            Integer minimo = null;
            Integer maximo = null;
            if (codigo == null
                    || !Cie10Normalizador.FORMATO_CODIGO.matcher(codigo).matches()) {
                problema = "El código no tiene el formato CIE-10, por ejemplo A00 o A00.0";
            } else if (!vistos.add(codigo)) {
                problema = "El código " + codigo + " está repetido en el archivo";
            } else if (descripcion.isEmpty()) {
                problema = "La descripción es obligatoria";
            } else if (descripcion.length() > 300) {
                problema = "La descripción supera 300 caracteres";
            } else if (capitulo.length() > 200) {
                problema = "El capítulo supera 200 caracteres";
            } else if (grupo.length() > 160) {
                problema = "El grupo supera 160 caracteres";
            }
            if (problema == null && !sexoTexto.isEmpty()) {
                sexo = sexo(sexoTexto);
                if (sexo == null) {
                    problema = "El sexo debe ser M o F, o quedar vacío";
                }
            }
            if (problema == null) {
                try {
                    minimo = entero(minimoTexto);
                    maximo = entero(maximoTexto);
                } catch (NumberFormatException e) {
                    problema = "Las edades deben ser números enteros entre 0 y 120";
                }
            }
            if (problema == null && minimo != null && maximo != null && minimo > maximo) {
                problema = "La edad mínima no puede ser mayor que la máxima";
            }
            if (problema != null) {
                errores.add(new Error(numero, problema));
            } else {
                validas.add(new Fila(
                        numero, codigo, descripcion, vacioANulo(capitulo), vacioANulo(grupo), sexo, minimo, maximo));
            }
        }
        return new Resultado(total, validas, errores);
    }

    private static String vacioANulo(String valor) {
        return valor.isEmpty() ? null : valor;
    }

    private static SexoPaciente sexo(String texto) {
        return switch (Cie10Normalizador.texto(texto)) {
            case "m", "masculino", "hombre" -> SexoPaciente.M;
            case "f", "femenino", "mujer" -> SexoPaciente.F;
            default -> null;
        };
    }

    private static Integer entero(String texto) {
        if (texto.isEmpty()) {
            return null;
        }
        int valor = Integer.parseInt(texto);
        if (valor < 0 || valor > 120) {
            throw new NumberFormatException();
        }
        return valor;
    }

    private static String valor(List<String> celdas, Map<String, Integer> columnas, String nombre) {
        Integer indice = columnas.get(nombre);
        return indice == null || indice >= celdas.size() ? "" : celdas.get(indice);
    }

    private static Map<String, Integer> columnas(List<String> cabecera, List<Error> errores) {
        Map<String, Integer> resultado = new HashMap<>();
        for (int i = 0; i < cabecera.size(); i++) {
            String clave = Cie10Normalizador.texto(cabecera.get(i)).replace(' ', '_');
            String nombre = switch (clave) {
                case "codigo", "cod", "code" -> "codigo";
                case "descripcion", "nombre", "diagnostico" -> "descripcion";
                case "capitulo" -> "capitulo";
                case "grupo", "bloque" -> "grupo";
                case "sexo" -> "sexo";
                case "edad_minima", "edad_min" -> "edad_minima";
                case "edad_maxima", "edad_max" -> "edad_maxima";
                default -> null;
            };
            if (nombre != null) {
                resultado.putIfAbsent(nombre, i);
            }
        }
        if (!resultado.containsKey("codigo") || !resultado.containsKey("descripcion")) {
            errores.add(new Error(1, "La cabecera debe incluir al menos las columnas codigo y descripcion"));
        }
        return resultado;
    }

    private static List<List<String>> leer(String texto) {
        char separador = separador(texto);
        List<List<String>> registros = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                        celda.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    celda.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == separador) {
                actual.add(celda.toString());
                celda.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                actual.add(celda.toString());
                celda.setLength(0);
                registros.add(actual);
                actual = new ArrayList<>();
            } else {
                celda.append(c);
            }
        }
        if (celda.length() > 0 || !actual.isEmpty()) {
            actual.add(celda.toString());
            registros.add(actual);
        }
        return registros;
    }

    private static char separador(String texto) {
        int fin = texto.indexOf('\n');
        String linea = fin < 0 ? texto : texto.substring(0, fin);
        long comas = linea.chars().filter(c -> c == ',').count();
        long puntoYComa = linea.chars().filter(c -> c == ';').count();
        long tabuladores = linea.chars().filter(c -> c == '\t').count();
        if (puntoYComa > comas && puntoYComa >= tabuladores) {
            return ';';
        }
        return tabuladores > comas ? '\t' : ',';
    }
}
