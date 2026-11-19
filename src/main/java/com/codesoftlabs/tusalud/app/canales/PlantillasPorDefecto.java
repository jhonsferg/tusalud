package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import java.util.EnumMap;
import java.util.Map;

public final class PlantillasPorDefecto {

    public record Contenido(String asunto, String cuerpo) {}

    private record Texto(String asunto, String frase) {}

    private static final Map<TipoNotificacion, Texto> TEXTOS = new EnumMap<>(TipoNotificacion.class);

    static {
        TEXTOS.put(
                TipoNotificacion.CITA_CREADA, new Texto("Su cita fue registrada", "Se registró una cita a su nombre."));
        TEXTOS.put(TipoNotificacion.CITA_CONFIRMADA, new Texto("Su cita fue confirmada", "Su cita fue confirmada."));
        TEXTOS.put(
                TipoNotificacion.CITA_CANCELADA,
                new Texto("Una cita fue cancelada", "Una de sus citas fue cancelada."));
        TEXTOS.put(
                TipoNotificacion.CITA_REPROGRAMADA,
                new Texto("Su cita fue reprogramada", "Una de sus citas cambió de fecha u hora."));
        TEXTOS.put(TipoNotificacion.RECORDATORIO_CITA, new Texto("Recordatorio de su cita", "Tiene una cita próxima."));
        TEXTOS.put(
                TipoNotificacion.TELECONSULTA_PROXIMA,
                new Texto("Su teleconsulta está por comenzar", "Su teleconsulta está próxima a comenzar."));
        TEXTOS.put(
                TipoNotificacion.TELECONSULTA_SALA_ABIERTA,
                new Texto("Su sala de teleconsulta está abierta", "Ya está abierta su sala de teleconsulta."));
        TEXTOS.put(
                TipoNotificacion.RESULTADO_LABORATORIO,
                new Texto("Tiene un resultado disponible", "Tiene un resultado nuevo disponible en su cuenta."));
        TEXTOS.put(
                TipoNotificacion.ESTUDIO_INFORMADO,
                new Texto("Tiene un informe disponible", "Tiene un informe nuevo disponible en su cuenta."));
        TEXTOS.put(
                TipoNotificacion.DOCUMENTO_EMITIDO,
                new Texto("Tiene un documento nuevo", "Se emitió un documento a su nombre."));
        TEXTOS.put(
                TipoNotificacion.COMPROBANTE_EMITIDO,
                new Texto("Tiene un comprobante nuevo", "Se emitió un comprobante de pago a su nombre."));
        TEXTOS.put(
                TipoNotificacion.ARCO_RESUELTA,
                new Texto(
                        "Respondimos su solicitud de privacidad",
                        "Su solicitud sobre sus datos personales ya tiene respuesta."));
        TEXTOS.put(
                TipoNotificacion.CONSENTIMIENTO_PENDIENTE,
                new Texto("Tiene un documento por firmar", "Tiene un documento pendiente de su firma."));
        TEXTOS.put(
                TipoNotificacion.CONTRASENA_CAMBIADA,
                new Texto(
                        "Se cambió su contraseña",
                        "La contraseña de su cuenta fue cambiada. Si no fue usted, comuníquese con soporte de inmediato."));
        TEXTOS.put(
                TipoNotificacion.MFA_ACTIVADO,
                new Texto(
                        "Se activó la verificación en dos pasos",
                        "Se activó la verificación en dos pasos en su cuenta. Si no fue usted, comuníquese con soporte."));
        TEXTOS.put(
                TipoNotificacion.MFA_DESACTIVADO,
                new Texto(
                        "Se desactivó la verificación en dos pasos",
                        "Se desactivó la verificación en dos pasos de su cuenta. Si no fue usted, comuníquese con soporte."));
        TEXTOS.put(
                TipoNotificacion.SESION_SOSPECHOSA,
                new Texto(
                        "Actividad inusual en su cuenta",
                        "Detectamos actividad inusual en su cuenta. Revise sus sesiones activas."));
        TEXTOS.put(
                TipoNotificacion.ROLES_ACTUALIZADOS,
                new Texto("Cambió el acceso de su cuenta", "Se modificaron los permisos de su cuenta."));
        TEXTOS.put(
                TipoNotificacion.RESPALDO_FALLIDO,
                new Texto(
                        "Un respaldo del sistema falló",
                        "Un respaldo o su prueba de restauración falló. Revise el estado de respaldos."));
        TEXTOS.put(
                TipoNotificacion.RESPALDO_VENCIDO,
                new Texto(
                        "Hay respaldos vencidos",
                        "No hay un respaldo reciente del sistema. Revise el estado de respaldos."));
    }

    private PlantillasPorDefecto() {}

    public static Contenido de(TipoNotificacion tipo, Canal canal) {
        Texto texto = TEXTOS.get(tipo);
        if (texto == null) {
            return null;
        }
        if (canal == Canal.CORREO) {
            return new Contenido(
                    texto.asunto(),
                    "Hola {{nombre}},\n\n" + texto.frase() + "\n\nIngrese a {{enlace}} para ver el detalle.\n\n"
                            + "Equipo de {{centro}}");
        }
        return new Contenido(null, "{{centro}}: " + texto.frase() + " {{enlace}}");
    }
}
