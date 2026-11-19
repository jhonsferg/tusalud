package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class ContenidoDocumentoMedico {

    private enum Clase {
        TEXTO,
        ENTERO,
        FECHA,
        UUID_ID,
        OPCION
    }

    private record Campo(
            String clave, String etiqueta, Clase clase, int maximo, boolean requerido, Set<String> opciones) {

        static Campo texto(String clave, String etiqueta, int maximo, boolean requerido) {
            return new Campo(clave, etiqueta, Clase.TEXTO, maximo, requerido, Set.of());
        }

        static Campo entero(String clave, String etiqueta, int minimo, int maximo) {
            return new Campo(clave, etiqueta, Clase.ENTERO, maximo, true, Set.of(String.valueOf(minimo)));
        }

        static Campo fecha(String clave, String etiqueta) {
            return new Campo(clave, etiqueta, Clase.FECHA, 10, true, Set.of());
        }

        static Campo identificador(String clave, String etiqueta) {
            return new Campo(clave, etiqueta, Clase.UUID_ID, 36, true, Set.of());
        }

        static Campo opcion(String clave, String etiqueta, String... valores) {
            return new Campo(clave, etiqueta, Clase.OPCION, 30, true, Set.of(valores));
        }
    }

    private static final Map<TipoDocumentoMedico, List<Campo>> CAMPOS = Map.of(
            TipoDocumentoMedico.CERTIFICADO,
            List.of(
                    Campo.texto("motivo", "motivo", 500, true),
                    Campo.texto("hallazgos", "hallazgos", 1000, false),
                    Campo.texto("recomendaciones", "recomendaciones", 1000, false),
                    Campo.entero("vigenciaDias", "vigencia en días", 1, 365)),
            TipoDocumentoMedico.DESCANSO,
            List.of(
                    Campo.opcion("tipoDescanso", "tipo de descanso", "ENFERMEDAD", "ACCIDENTE", "MATERNIDAD", "OTROS"),
                    Campo.fecha("desde", "fecha de inicio"),
                    Campo.fecha("hasta", "fecha de fin"),
                    Campo.texto("contingencia", "contingencia", 200, true),
                    Campo.texto("indicacion", "indicación de reposo", 500, true)),
            TipoDocumentoMedico.INFORME,
            List.of(
                    Campo.texto("destinatario", "destinatario", 150, true),
                    Campo.texto("resumen", "resumen clínico", 2000, true),
                    Campo.texto("tratamiento", "tratamiento", 1500, false),
                    Campo.texto("evolucion", "evolución", 1500, false)),
            TipoDocumentoMedico.REFERENCIA,
            List.of(
                    Campo.identificador("destinoId", "establecimiento de destino"),
                    Campo.texto("especialidad", "especialidad requerida", 100, true),
                    Campo.texto("motivo", "motivo", 500, true),
                    Campo.texto("resumenClinico", "resumen clínico", 2000, true),
                    Campo.opcion("prioridad", "prioridad", "NORMAL", "PREFERENTE", "URGENTE")),
            TipoDocumentoMedico.CONTRARREFERENCIA,
            List.of(
                    Campo.texto("resumen", "resumen de la atención", 2000, true),
                    Campo.texto("recomendaciones", "recomendaciones", 1000, false)),
            TipoDocumentoMedico.EPICRISIS,
            List.of(
                    Campo.texto("motivo", "motivo de ingreso", 500, true),
                    Campo.texto("diagnosticoIngreso", "diagnóstico de ingreso", 500, false),
                    Campo.texto("diagnosticoAlta", "diagnóstico de alta", 500, false),
                    Campo.texto("resumenEvolucion", "resumen de la evolución", 2000, true),
                    Campo.texto("procedimientos", "procedimientos", 1000, false),
                    Campo.opcion(
                            "condicionAlta", "condición de alta", "MEJORADO", "ESTACIONARIO", "AGRAVADO", "FALLECIDO"),
                    Campo.texto("indicaciones", "indicaciones de alta", 1000, true)));

    private ContenidoDocumentoMedico() {}

    public static Map<String, Object> normalizar(TipoDocumentoMedico tipo, Map<String, Object> entrada) {
        Map<String, Object> resultado = new TreeMap<>();
        for (Campo campo : CAMPOS.get(tipo)) {
            Object crudo = entrada == null ? null : entrada.get(campo.clave());
            String texto = crudo == null ? "" : limpiar(String.valueOf(crudo));
            if (texto.isEmpty()) {
                if (campo.requerido()) {
                    throw new DatosInvalidosException("Indique " + campo.etiqueta());
                }
                continue;
            }
            if (texto.length() > campo.maximo()) {
                throw new DatosInvalidosException(
                        "El campo " + campo.etiqueta() + " no puede superar los " + campo.maximo() + " caracteres");
            }
            resultado.put(campo.clave(), convertir(campo, texto));
        }
        return new LinkedHashMap<>(resultado);
    }

    private static Object convertir(Campo campo, String texto) {
        switch (campo.clase()) {
            case ENTERO -> {
                try {
                    int valor = Integer.parseInt(texto);
                    int minimo = Integer.parseInt(campo.opciones().iterator().next());
                    if (valor < minimo || valor > campo.maximo()) {
                        throw new DatosInvalidosException("El campo " + campo.etiqueta() + " debe estar entre " + minimo
                                + " y " + campo.maximo());
                    }
                    return valor;
                } catch (NumberFormatException ex) {
                    throw new DatosInvalidosException("El campo " + campo.etiqueta() + " debe ser un número");
                }
            }
            case FECHA -> {
                try {
                    return LocalDate.parse(texto).toString();
                } catch (DateTimeParseException ex) {
                    throw new DatosInvalidosException("La " + campo.etiqueta() + " no es una fecha válida");
                }
            }
            case UUID_ID -> {
                try {
                    return UUID.fromString(texto).toString();
                } catch (IllegalArgumentException ex) {
                    throw new DatosInvalidosException("El valor de " + campo.etiqueta() + " no es válido");
                }
            }
            case OPCION -> {
                if (!campo.opciones().contains(texto)) {
                    throw new DatosInvalidosException("El valor de " + campo.etiqueta() + " no es válido");
                }
                return texto;
            }
            default -> {
                return texto;
            }
        }
    }

    private static String limpiar(String texto) {
        StringBuilder limpio = new StringBuilder(texto.length());
        for (char c : texto.strip().toCharArray()) {
            if (c == '\n' || c == '\t' || c >= 32 && c != 127) {
                limpio.append(c);
            }
        }
        return limpio.toString().strip();
    }
}
