import { EstadoOrden } from '@tusalud/core';

export type Severidad = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

export const SEVERIDAD_ORDEN: Readonly<Record<EstadoOrden, Severidad>> = {
  SOLICITADA: 'warn',
  MUESTRA_TOMADA: 'info',
  EN_PROCESO: 'info',
  COMPLETADA: 'success',
  CANCELADA: 'secondary',
};

export const ICONO_ORDEN: Readonly<Record<EstadoOrden, string>> = {
  SOLICITADA: 'pi-clock',
  MUESTRA_TOMADA: 'pi-filter',
  EN_PROCESO: 'pi-spinner',
  COMPLETADA: 'pi-check-circle',
  CANCELADA: 'pi-times-circle',
};

export function rangoReferencia(
  min?: number | null,
  max?: number | null,
  unidad?: string | null,
): string {
  const sufijo = unidad ? ` ${unidad}` : '';
  if (min != null && max != null) {
    return `${min} - ${max}${sufijo}`;
  }
  if (min != null) {
    return `≥ ${min}${sufijo}`;
  }
  if (max != null) {
    return `≤ ${max}${sufijo}`;
  }
  return '—';
}
