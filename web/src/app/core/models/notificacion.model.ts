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
  | 'SISTEMA';

export interface NotificacionResponse {
  id: number;
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
