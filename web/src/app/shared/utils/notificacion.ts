import { TipoNotificacion } from '@tusalud/app/core/models';

export const ICONO_NOTIFICACION: Readonly<Record<TipoNotificacion, string>> = {
  CITA_CREADA: 'pi-calendar-plus',
  CITA_CONFIRMADA: 'pi-check-circle',
  CITA_CANCELADA: 'pi-times-circle',
  CITA_ATENDIDA: 'pi-verified',
  CITA_REPROGRAMADA: 'pi-refresh',
  RECORDATORIO_CITA: 'pi-bell',
  INICIO_SESION: 'pi-shield',
  CONTRASENA_CAMBIADA: 'pi-key',
  PAGO_REGISTRADO: 'pi-credit-card',
  PAGO_REEMBOLSADO: 'pi-replay',
  SISTEMA: 'pi-info-circle',
};

export const COLOR_NOTIFICACION: Readonly<Record<TipoNotificacion, string>> = {
  CITA_CREADA: '#3b82f6',
  CITA_CONFIRMADA: '#10b981',
  CITA_CANCELADA: '#ef4444',
  CITA_ATENDIDA: '#10b981',
  CITA_REPROGRAMADA: '#f59e0b',
  RECORDATORIO_CITA: '#8b5cf6',
  INICIO_SESION: '#f59e0b',
  CONTRASENA_CAMBIADA: '#f59e0b',
  PAGO_REGISTRADO: '#10b981',
  PAGO_REEMBOLSADO: '#0ea5e9',
  SISTEMA: '#64748b',
};
