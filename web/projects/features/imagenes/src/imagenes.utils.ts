import { OrdenImagenResponse } from '@tusalud/core';

export type Severidad = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

export const SEVERIDAD_IMAGEN: Readonly<Record<string, Severidad>> = {
  SOLICITADA: 'warn',
  PROGRAMADA: 'info',
  REALIZADA: 'warn',
  INFORMADA: 'success',
  CANCELADA: 'secondary',
};

export const ICONO_ESTADO_IMAGEN: Readonly<Record<string, string>> = {
  SOLICITADA: 'pi-clock',
  PROGRAMADA: 'pi-calendar',
  REALIZADA: 'pi-hourglass',
  INFORMADA: 'pi-check-circle',
  CANCELADA: 'pi-times-circle',
};

export function tamanoLegible(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function resumenEstudios(orden: OrdenImagenResponse): string {
  return orden.estudios.map((estudio) => estudio.nombre).join(', ');
}
