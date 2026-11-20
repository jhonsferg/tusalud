import {
  EstadoItemPlan,
  EstadoPieza,
  EstadoPlanOdontologico,
  PiezaOdontograma,
  SuperficieDental,
} from '@tusalud/core';

export type SeveridadEtiqueta = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

export const SUPERFICIES: readonly SuperficieDental[] = ['V', 'L', 'M', 'D', 'O'];

export const ETIQUETA_SUPERFICIE: Record<SuperficieDental, string> = {
  V: 'Vestibular',
  L: 'Lingual o palatina',
  M: 'Mesial',
  D: 'Distal',
  O: 'Oclusal o incisal',
};

export const ETIQUETA_ESTADO_PIEZA: Record<EstadoPieza, string> = {
  SANO: 'Sano',
  CARIES: 'Caries',
  OBTURADO: 'Obturado',
  SELLANTE: 'Sellante',
  FRACTURA: 'Fractura',
  CORONA: 'Corona',
  ENDODONCIA: 'Endodoncia',
  IMPLANTE: 'Implante',
  PROTESIS: 'Prótesis',
  EXTRACCION_INDICADA: 'Extracción indicada',
  AUSENTE: 'Ausente',
};

export const ESTADOS_POR_SUPERFICIE: readonly EstadoPieza[] = [
  'SANO',
  'CARIES',
  'OBTURADO',
  'SELLANTE',
  'FRACTURA',
];

export const ESTADOS_PIEZA_COMPLETA: readonly EstadoPieza[] = [
  'SANO',
  'CORONA',
  'ENDODONCIA',
  'IMPLANTE',
  'PROTESIS',
  'EXTRACCION_INDICADA',
  'AUSENTE',
];

export const COLOR_ESTADO_PIEZA: Record<EstadoPieza, string> = {
  SANO: '#ffffff',
  CARIES: '#dc2626',
  OBTURADO: '#2563eb',
  SELLANTE: '#16a34a',
  FRACTURA: '#f59e0b',
  CORONA: '#a855f7',
  ENDODONCIA: '#db2777',
  IMPLANTE: '#0891b2',
  PROTESIS: '#7c3aed',
  EXTRACCION_INDICADA: '#ea580c',
  AUSENTE: '#94a3b8',
};

export const ETIQUETA_ESTADO_PLAN: Record<EstadoPlanOdontologico, string> = {
  BORRADOR: 'Borrador',
  PRESENTADO: 'Presentado',
  ACEPTADO: 'Aceptado',
  RECHAZADO: 'Rechazado',
  EN_CURSO: 'En curso',
  COMPLETADO: 'Completado',
  CANCELADO: 'Cancelado',
};

export const SEVERIDAD_ESTADO_PLAN: Record<EstadoPlanOdontologico, SeveridadEtiqueta> = {
  BORRADOR: 'secondary',
  PRESENTADO: 'info',
  ACEPTADO: 'success',
  RECHAZADO: 'danger',
  EN_CURSO: 'warn',
  COMPLETADO: 'success',
  CANCELADO: 'contrast',
};

export const ETIQUETA_ESTADO_ITEM: Record<EstadoItemPlan, string> = {
  PENDIENTE: 'Pendiente',
  REALIZADO: 'Realizado',
  CANCELADO: 'Cancelado',
};

export const METODOS_PAGO: readonly string[] = [
  'EFECTIVO',
  'TARJETA',
  'YAPE',
  'PLIN',
  'TRANSFERENCIA',
];

export const FILAS_PERMANENTES: readonly (readonly number[])[] = [
  [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28],
  [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38],
];

export const FILAS_TEMPORALES: readonly (readonly number[])[] = [
  [55, 54, 53, 52, 51, 61, 62, 63, 64, 65],
  [85, 84, 83, 82, 81, 71, 72, 73, 74, 75],
];

export function piezaValida(pieza: number): boolean {
  const cuadrante = Math.floor(pieza / 10);
  const posicion = pieza % 10;
  if (cuadrante >= 1 && cuadrante <= 4) {
    return posicion >= 1 && posicion <= 8;
  }
  if (cuadrante >= 5 && cuadrante <= 8) {
    return posicion >= 1 && posicion <= 5;
  }
  return false;
}

export function esTemporal(pieza: number): boolean {
  return Math.floor(pieza / 10) >= 5;
}

export function estadoVisible(pieza: PiezaOdontograma | undefined): EstadoPieza {
  return pieza?.estado ?? 'SANO';
}

export function estadoDeSuperficie(
  pieza: PiezaOdontograma | undefined,
  superficie: SuperficieDental,
): EstadoPieza {
  return pieza?.superficies[superficie] ?? 'SANO';
}

export function descripcionPieza(numero: number, pieza: PiezaOdontograma | undefined): string {
  const base = `Pieza ${numero}${esTemporal(numero) ? ', temporal' : ''}`;
  if (!pieza) {
    return `${base}, sin registros`;
  }
  const partes: string[] = [];
  if (pieza.estado) {
    partes.push(ETIQUETA_ESTADO_PIEZA[pieza.estado]);
  }
  for (const s of SUPERFICIES) {
    const estado = pieza.superficies[s];
    if (estado && estado !== 'SANO') {
      partes.push(`${ETIQUETA_SUPERFICIE[s]} ${ETIQUETA_ESTADO_PIEZA[estado].toLowerCase()}`);
    }
  }
  return partes.length ? `${base}, ${partes.join(', ')}` : `${base}, sano`;
}

export function vecina(
  numero: number,
  filas: readonly (readonly number[])[],
  dx: number,
  dy: number,
): number | null {
  for (let f = 0; f < filas.length; f++) {
    const c = filas[f].indexOf(numero);
    if (c < 0) {
      continue;
    }
    const destino = f + dy;
    if (destino < 0 || destino >= filas.length) {
      return null;
    }
    const columna = Math.min(Math.max(c + dx, 0), filas[destino].length - 1);
    if (destino === f && columna === c) {
      return null;
    }
    return filas[destino][columna];
  }
  return null;
}

export type PoligonosDiente = Record<SuperficieDental, string>;

export function poligonosDiente(numero: number): PoligonosDiente {
  const cuadrante = Math.floor(numero / 10);
  const superior = cuadrante === 1 || cuadrante === 2 || cuadrante === 5 || cuadrante === 6;
  const mesialDerecha = cuadrante === 1 || cuadrante === 4 || cuadrante === 5 || cuadrante === 8;
  const arriba = '4,4 36,4 28,12 12,12';
  const abajo = '4,36 36,36 28,28 12,28';
  const izquierda = '4,4 12,12 12,28 4,36';
  const derecha = '36,4 28,12 28,28 36,36';
  return {
    V: superior ? arriba : abajo,
    L: superior ? abajo : arriba,
    M: mesialDerecha ? derecha : izquierda,
    D: mesialDerecha ? izquierda : derecha,
    O: '12,12 28,12 28,28 12,28',
  };
}

export function fechaLegible(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const [anio, mes, dia] = iso.substring(0, 10).split('-');
  return `${dia}/${mes}/${anio}`;
}

export function soles(valor: number): string {
  return `S/ ${valor.toFixed(2)}`;
}
