package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;

public final class Etiquetas {

    private Etiquetas() {}

    public static String metodoPago(MetodoPagoCaja metodo) {
        if (metodo == null) {
            return "";
        }
        return switch (metodo) {
            case EFECTIVO -> "Efectivo";
            case TARJETA -> "Tarjeta";
            case YAPE -> "Yape";
            case PLIN -> "Plin";
            case TRANSFERENCIA -> "Transferencia";
        };
    }

    public static String metodoPago(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            return "";
        }
        try {
            return metodoPago(MetodoPagoCaja.valueOf(metodo));
        } catch (IllegalArgumentException ex) {
            return metodo;
        }
    }

    public static String origen(OrigenComprobante origen) {
        if (origen == null) {
            return "";
        }
        return switch (origen) {
            case CITA -> "Consulta";
            case FARMACIA -> "Farmacia";
            case LABORATORIO -> "Laboratorio";
            case IMAGEN -> "Imágenes diagnósticas";
            case SERVICIO -> "Servicio";
            case OTRO -> "Otro";
        };
    }

    public static String origen(String origen) {
        if (origen == null || origen.isBlank()) {
            return "";
        }
        try {
            return origen(OrigenComprobante.valueOf(origen));
        } catch (IllegalArgumentException ex) {
            return origen;
        }
    }

    public static String estadoCita(EstadoCita estado) {
        if (estado == null) {
            return "";
        }
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case CONFIRMADA -> "Confirmada";
            case ATENDIDA -> "Atendida";
            case CANCELADA -> "Cancelada";
        };
    }
}
