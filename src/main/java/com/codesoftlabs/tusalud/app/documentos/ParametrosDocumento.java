package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

public final class ParametrosDocumento {

    public static final long DIAS_MAXIMOS = 366;
    private static final int TEXTO_MAXIMO = 80;

    private final Map<String, String> valores;

    public ParametrosDocumento(Map<String, String> valores) {
        this.valores = valores == null ? Map.of() : valores;
    }

    public String texto(String clave) {
        String valor = valores.get(clave);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.strip();
        if (limpio.length() > TEXTO_MAXIMO) {
            throw new DatosInvalidosException("El parámetro " + clave + " es demasiado largo");
        }
        return limpio;
    }

    public LocalDate fecha(String clave) {
        String valor = texto(clave);
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new DatosInvalidosException("El parámetro " + clave + " debe tener el formato aaaa-mm-dd");
        }
    }

    public UUID uuid(String clave) {
        String valor = texto(clave);
        if (valor == null) {
            return null;
        }
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El parámetro " + clave + " no es un identificador válido");
        }
    }

    public <E extends Enum<E>> E enumeracion(String clave, Class<E> tipo) {
        String valor = texto(clave);
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El parámetro " + clave + " tiene un valor no permitido");
        }
    }

    public Boolean booleano(String clave) {
        String valor = texto(clave);
        return valor == null ? null : Boolean.valueOf(valor);
    }

    public LocalDate[] rango(LocalDate defectoDesde, LocalDate defectoHasta) {
        LocalDate desde = fecha("desde");
        LocalDate hasta = fecha("hasta");
        if (hasta == null) {
            hasta = defectoHasta;
        }
        if (desde == null) {
            desde = defectoDesde;
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) + 1 > DIAS_MAXIMOS) {
            throw new DatosInvalidosException("El rango no puede superar los 366 días");
        }
        return new LocalDate[] {desde, hasta};
    }

    public Map<String, String> comoMapa() {
        return valores;
    }
}
