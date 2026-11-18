import {
  EstadoCuentaAseguradora,
  EstadoLiquidacion,
  EstadoPoliza,
  EstadoPreautorizacion,
} from '@tusalud/core';

export type Severidad = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

export const SEVERIDAD_POLIZA: Readonly<Record<string, Severidad>> = {
  ACTIVA: 'success',
  VENCIDA: 'danger',
  SUSPENDIDA: 'warn',
};

export const SEVERIDAD_PREAUTORIZACION: Readonly<Record<string, Severidad>> = {
  SOLICITADA: 'info',
  APROBADA: 'success',
  RECHAZADA: 'danger',
  VENCIDA: 'secondary',
};

export const SEVERIDAD_LIQUIDACION: Readonly<Record<string, Severidad>> = {
  BORRADOR: 'secondary',
  ENVIADA: 'info',
  PAGADA: 'success',
  OBSERVADA: 'warn',
};

export const SEVERIDAD_CUENTA: Readonly<Record<string, Severidad>> = {
  PENDIENTE: 'warn',
  LIQUIDADA: 'info',
  PAGADA: 'success',
  REVERTIDA: 'secondary',
};

export function hoyIso(desplazamientoDias = 0): string {
  const fecha = new Date(Date.now() + desplazamientoDias * 86_400_000);
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

export function textoCobertura(tipo: string, valor: number): string {
  return tipo === 'PORCENTAJE' ? `${valor}% cubierto` : `Copago fijo de S/ ${valor.toFixed(2)}`;
}
