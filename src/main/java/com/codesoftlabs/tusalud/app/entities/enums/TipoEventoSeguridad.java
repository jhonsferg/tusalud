package com.codesoftlabs.tusalud.app.entities.enums;

public enum TipoEventoSeguridad {
    LOGIN_FALLIDO(SeveridadSeguridad.AVISO, "Inicio de sesión fallido"),
    LOGIN_CUENTA_INACTIVA(SeveridadSeguridad.AVISO, "Intento con cuenta inactiva"),
    LOGIN_FALLIDOS_REPETIDOS(SeveridadSeguridad.ALTA, "Inicios de sesión fallidos repetidos"),
    DISPOSITIVO_NUEVO(SeveridadSeguridad.INFO, "Inicio de sesión desde un dispositivo nuevo"),
    BLOQUEO_INTENTOS(SeveridadSeguridad.ALTA, "Bloqueo por intentos de verificación"),
    MFA_ACTIVADO(SeveridadSeguridad.INFO, "Verificación en dos pasos activada"),
    MFA_DESACTIVADO(SeveridadSeguridad.AVISO, "Verificación en dos pasos desactivada"),
    CONTRASENA_CAMBIADA(SeveridadSeguridad.INFO, "Contraseña cambiada"),
    ROLES_CAMBIADOS(SeveridadSeguridad.AVISO, "Roles modificados"),
    CUENTA_ACTIVADA(SeveridadSeguridad.INFO, "Cuenta activada"),
    CUENTA_DESACTIVADA(SeveridadSeguridad.AVISO, "Cuenta desactivada"),
    ACCESO_DENEGADO(SeveridadSeguridad.INFO, "Acceso denegado"),
    ACCESO_DENEGADO_REPETIDO(SeveridadSeguridad.ALTA, "Accesos denegados repetidos"),
    DESCARGA_MASIVA(SeveridadSeguridad.ALTA, "Descarga masiva de archivos");

    private final SeveridadSeguridad severidad;
    private final String etiqueta;

    TipoEventoSeguridad(SeveridadSeguridad severidad, String etiqueta) {
        this.severidad = severidad;
        this.etiqueta = etiqueta;
    }

    public SeveridadSeguridad getSeveridad() {
        return severidad;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
