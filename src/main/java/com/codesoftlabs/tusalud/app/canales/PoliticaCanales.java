package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PoliticaCanales {

    public record Politica(boolean obligatoria, boolean urgente, String categoria, String etiqueta) {}

    private static final Map<TipoNotificacion, Politica> POLITICAS = new EnumMap<>(TipoNotificacion.class);

    static {
        registrar(TipoNotificacion.CITA_CREADA, false, false, "Citas", "Cita registrada");
        registrar(TipoNotificacion.CITA_CONFIRMADA, false, false, "Citas", "Cita confirmada");
        registrar(TipoNotificacion.CITA_CANCELADA, false, false, "Citas", "Cita cancelada");
        registrar(TipoNotificacion.CITA_REPROGRAMADA, false, false, "Citas", "Cita reprogramada");
        registrar(TipoNotificacion.RECORDATORIO_CITA, false, false, "Citas", "Recordatorio de cita");
        registrar(TipoNotificacion.TELECONSULTA_PROXIMA, false, true, "Teleconsulta", "Teleconsulta próxima");
        registrar(
                TipoNotificacion.TELECONSULTA_SALA_ABIERTA,
                false,
                true,
                "Teleconsulta",
                "Sala de teleconsulta abierta");
        registrar(TipoNotificacion.RESULTADO_LABORATORIO, false, false, "Resultados", "Resultado disponible");
        registrar(TipoNotificacion.ESTUDIO_INFORMADO, false, false, "Resultados", "Informe disponible");
        registrar(TipoNotificacion.DOCUMENTO_EMITIDO, false, false, "Documentos", "Documento emitido");
        registrar(TipoNotificacion.CONSENTIMIENTO_PENDIENTE, false, false, "Documentos", "Consentimiento por firmar");
        registrar(TipoNotificacion.COMPROBANTE_EMITIDO, false, false, "Pagos", "Comprobante emitido");
        registrar(TipoNotificacion.ARCO_RESUELTA, false, false, "Privacidad", "Respuesta a su solicitud de privacidad");
        registrar(TipoNotificacion.CONTRASENA_CAMBIADA, true, true, "Seguridad", "Cambio de contraseña");
        registrar(TipoNotificacion.MFA_ACTIVADO, true, true, "Seguridad", "Verificación en dos pasos activada");
        registrar(TipoNotificacion.MFA_DESACTIVADO, true, true, "Seguridad", "Verificación en dos pasos desactivada");
        registrar(TipoNotificacion.SESION_SOSPECHOSA, true, true, "Seguridad", "Actividad inusual");
        registrar(TipoNotificacion.ROLES_ACTUALIZADOS, true, true, "Seguridad", "Cambio de accesos");
        registrar(TipoNotificacion.RESPALDO_FALLIDO, true, true, "Seguridad", "Respaldo fallido");
        registrar(TipoNotificacion.RESPALDO_VENCIDO, true, true, "Seguridad", "Respaldo vencido");
    }

    private PoliticaCanales() {}

    private static void registrar(
            TipoNotificacion tipo, boolean obligatoria, boolean urgente, String categoria, String etiqueta) {
        POLITICAS.put(tipo, new Politica(obligatoria, urgente, categoria, etiqueta));
    }

    public static Optional<Politica> de(TipoNotificacion tipo) {
        return Optional.ofNullable(POLITICAS.get(tipo));
    }

    public static boolean externa(TipoNotificacion tipo) {
        return POLITICAS.containsKey(tipo);
    }

    public static List<TipoNotificacion> tiposExternos() {
        return List.copyOf(POLITICAS.keySet());
    }

    public static boolean activoPorDefecto(TipoNotificacion tipo, Canal canal) {
        if (canal == Canal.CORREO) {
            return true;
        }
        Politica politica = POLITICAS.get(tipo);
        return politica != null
                && (politica.obligatoria()
                        || tipo == TipoNotificacion.RECORDATORIO_CITA
                        || tipo == TipoNotificacion.TELECONSULTA_PROXIMA
                        || tipo == TipoNotificacion.TELECONSULTA_SALA_ABIERTA);
    }
}
