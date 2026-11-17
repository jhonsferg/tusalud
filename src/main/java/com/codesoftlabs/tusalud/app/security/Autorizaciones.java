package com.codesoftlabs.tusalud.app.security;

public final class Autorizaciones {

    public static final String PERSONAL = "hasAnyRole('ADMIN', 'MEDICO', 'LABORATORIO', 'FARMACIA', 'ALMACEN', "
            + "'RECEPCION', 'CAJA', 'ENFERMERIA', 'GERENCIA', 'FINANZAS', 'OPERACIONES', 'RRHH', 'CALIDAD', 'IMAGENES')";

    private Autorizaciones() {}
}
