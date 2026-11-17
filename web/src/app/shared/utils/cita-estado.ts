import { EstadoCita } from '@tusalud/app/core/models';

export type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

export const ETIQUETA_ESTADO: Readonly<Record<EstadoCita, string>> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
};

export const SEVERIDAD_ESTADO: Readonly<Record<EstadoCita, TagSeverity>> = {
  PENDIENTE: 'warn',
  CONFIRMADA: 'info',
  ATENDIDA: 'success',
  CANCELADA: 'danger',
};

export const ICONO_ESTADO: Readonly<Record<EstadoCita, string>> = {
  PENDIENTE: 'pi-clock',
  CONFIRMADA: 'pi-check-circle',
  ATENDIDA: 'pi-verified',
  CANCELADA: 'pi-times-circle',
};

export const COLOR_ESTADO: Readonly<Record<EstadoCita, string>> = {
  PENDIENTE: '#f59e0b',
  CONFIRMADA: '#0ea5e9',
  ATENDIDA: '#10b981',
  CANCELADA: '#ef4444',
};
