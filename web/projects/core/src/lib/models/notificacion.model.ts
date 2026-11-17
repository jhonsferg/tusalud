export type TipoNotificacion =
  | 'CITA_CREADA'
  | 'CITA_CONFIRMADA'
  | 'CITA_CANCELADA'
  | 'CITA_ATENDIDA'
  | 'CITA_REPROGRAMADA'
  | 'RECORDATORIO_CITA'
  | 'INICIO_SESION'
  | 'CONTRASENA_CAMBIADA'
  | 'PAGO_REGISTRADO'
  | 'PAGO_REEMBOLSADO'
  | 'MFA_ACTIVADO'
  | 'MFA_DESACTIVADO'
  | 'PACIENTE_COMPARTIDO'
  | 'ROLES_ACTUALIZADOS'
  | 'SESION_SOSPECHOSA'
  | 'RECORDATORIO_EVENTO'
  | 'PACIENTE_EN_ESPERA'
  | 'TURNO_LLAMADO'
  | 'COMPROBANTE_EMITIDO'
  | 'INGRESO_HOSPITALARIO'
  | 'ALTA_HOSPITALARIA'
  | 'EMERGENCIA_TRIAJE'
  | 'CAJA_DIFERENCIA'
  | 'SISTEMA';

export interface NotificacionResponse {
  id: string;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje: string;
  enlace?: string | null;
  leida: boolean;
  createdAt: string;
}

export interface NotificacionResumen {
  noLeidas: number;
}
