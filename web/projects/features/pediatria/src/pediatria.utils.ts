import {
  CurvaCrecimiento,
  EstadoDosis,
  IndicadorCrecimiento,
  NivelAlerta,
  ReferenciaPunto,
} from '@tusalud/core';

export type SeveridadEtiqueta = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

export const ETIQUETA_ESTADO_DOSIS: Record<EstadoDosis, string> = {
  APLICADA: 'Aplicada',
  ATRASADA: 'Atrasada',
  PROXIMA: 'Próxima',
  PENDIENTE: 'Pendiente',
  FUERA_DE_EDAD: 'Fuera de edad',
};

export const SEVERIDAD_ESTADO_DOSIS: Record<EstadoDosis, SeveridadEtiqueta> = {
  APLICADA: 'success',
  ATRASADA: 'danger',
  PROXIMA: 'warn',
  PENDIENTE: 'secondary',
  FUERA_DE_EDAD: 'contrast',
};

export const ETIQUETA_NIVEL: Record<NivelAlerta, string> = {
  NORMAL: 'Normal',
  ATENCION: 'Vigilar',
  ALERTA: 'Alerta',
};

export const SEVERIDAD_NIVEL: Record<NivelAlerta, SeveridadEtiqueta> = {
  NORMAL: 'success',
  ATENCION: 'warn',
  ALERTA: 'danger',
};

export const COLOR_NIVEL: Record<NivelAlerta, string> = {
  NORMAL: '#15803d',
  ATENCION: '#b45309',
  ALERTA: '#b91c1c',
};

export const ETIQUETA_INDICADOR: Record<IndicadorCrecimiento, string> = {
  PESO_EDAD: 'Peso para la edad',
  TALLA_EDAD: 'Talla para la edad',
  PC_EDAD: 'Perímetro cefálico para la edad',
  IMC_EDAD: 'Índice de masa corporal para la edad',
};

export function fechaLegible(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const [anio, mes, dia] = iso.substring(0, 10).split('-');
  return `${dia}/${mes}/${anio}`;
}

export function edadLegible(meses: number | null | undefined): string {
  if (meses === null || meses === undefined) {
    return 'Edad no registrada';
  }
  if (meses < 24) {
    return `${meses} ${meses === 1 ? 'mes' : 'meses'}`;
  }
  const anios = Math.floor(meses / 12);
  const resto = meses % 12;
  return resto === 0
    ? `${anios} años`
    : `${anios} años y ${resto} ${resto === 1 ? 'mes' : 'meses'}`;
}

export interface Escalas {
  readonly xMin: number;
  readonly xMax: number;
  readonly yMin: number;
  readonly yMax: number;
}

export interface Lienzo {
  readonly ancho: number;
  readonly alto: number;
  readonly margen: number;
}

export function referenciaVisible(
  referencia: readonly ReferenciaPunto[],
  edadMaxima: number,
): ReferenciaPunto[] {
  const dentro = referencia.filter((r) => r.edadMeses <= edadMaxima);
  const siguiente = referencia.find((r) => r.edadMeses > edadMaxima);
  return siguiente ? [...dentro, siguiente] : dentro;
}

export function escalasDeCurva(curva: CurvaCrecimiento): Escalas | null {
  const edades = curva.puntos.map((p) => p.edadMeses);
  const maxEdad = edades.length ? Math.max(...edades) : 12;
  const ref = referenciaVisible(curva.referencia, maxEdad * 1.15 + 6);
  if (ref.length < 2) {
    return null;
  }
  const valores = [...ref.flatMap((r) => [r.p3, r.p97]), ...curva.puntos.map((p) => p.valor)];
  const yMin = Math.min(...valores);
  const yMax = Math.max(...valores);
  const holgura = (yMax - yMin) * 0.06 || 1;
  return {
    xMin: ref[0].edadMeses,
    xMax: ref[ref.length - 1].edadMeses,
    yMin: yMin - holgura,
    yMax: yMax + holgura,
  };
}

export function posicionX(edad: number, escalas: Escalas, lienzo: Lienzo): number {
  const rango = escalas.xMax - escalas.xMin || 1;
  return lienzo.margen + ((edad - escalas.xMin) / rango) * (lienzo.ancho - 2 * lienzo.margen);
}

export function posicionY(valor: number, escalas: Escalas, lienzo: Lienzo): number {
  const rango = escalas.yMax - escalas.yMin || 1;
  return (
    lienzo.alto -
    lienzo.margen -
    ((valor - escalas.yMin) / rango) * (lienzo.alto - 2 * lienzo.margen)
  );
}

export function trazo(puntos: readonly { x: number; y: number }[]): string {
  return puntos
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
    .join(' ');
}

export function resumenAccesible(curva: CurvaCrecimiento): string {
  if (curva.puntos.length === 0) {
    return `${curva.etiqueta}: sin mediciones registradas`;
  }
  const ultimo = curva.puntos[curva.puntos.length - 1];
  const percentil =
    ultimo.percentil === null ? 'sin referencia' : `percentil ${Math.round(ultimo.percentil)}`;
  const cantidad = curva.puntos.length;
  const palabra = cantidad === 1 ? 'medición' : 'mediciones';
  return `${curva.etiqueta}: ${cantidad} ${palabra}, la última de ${ultimo.valor} ${curva.unidad}, ${percentil}`;
}
