import {
  AlertaReceta,
  EstadoRecetaElectronica,
  RecetaFarmacia,
  SeveridadAlerta,
  TipoAlertaReceta,
} from '@tusalud/core';

export type SeveridadEtiqueta = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

export const ETIQUETA_ESTADO_RECETA: Readonly<Record<EstadoRecetaElectronica, string>> = {
  EMITIDA: 'Emitida',
  DISPENSADA_PARCIAL: 'Dispensada en parte',
  DISPENSADA: 'Dispensada',
  ANULADA: 'Anulada',
};

export const SEVERIDAD_ESTADO_RECETA: Readonly<Record<EstadoRecetaElectronica, SeveridadEtiqueta>> =
  {
    EMITIDA: 'success',
    DISPENSADA_PARCIAL: 'warn',
    DISPENSADA: 'info',
    ANULADA: 'danger',
  };

export const ETIQUETA_SEVERIDAD_ALERTA: Readonly<Record<SeveridadAlerta, string>> = {
  INFORMATIVA: 'Informativa',
  MODERADA: 'Moderada',
  GRAVE: 'Grave',
  CONTRAINDICADA: 'Contraindicada',
};

export const SEVERIDAD_ALERTA: Readonly<Record<SeveridadAlerta, SeveridadEtiqueta>> = {
  INFORMATIVA: 'secondary',
  MODERADA: 'warn',
  GRAVE: 'danger',
  CONTRAINDICADA: 'danger',
};

export const ETIQUETA_TIPO_ALERTA: Readonly<Record<TipoAlertaReceta, string>> = {
  ALERGIA: 'Alergia',
  INTERACCION: 'Interacción',
  DUPLICIDAD: 'Duplicidad',
  DOSIS_MAXIMA: 'Dosis máxima',
  EMBARAZO: 'Embarazo',
  LACTANCIA: 'Lactancia',
  RENAL: 'Función renal',
};

const ORDEN_SEVERIDAD: Readonly<Record<SeveridadAlerta, number>> = {
  CONTRAINDICADA: 0,
  GRAVE: 1,
  MODERADA: 2,
  INFORMATIVA: 3,
};

export function ordenarAlertas(alertas: readonly AlertaReceta[]): AlertaReceta[] {
  return [...alertas].sort(
    (a, b) =>
      ORDEN_SEVERIDAD[a.severidad] - ORDEN_SEVERIDAD[b.severidad] ||
      a.tipo.localeCompare(b.tipo) ||
      a.clave.localeCompare(b.clave),
  );
}

export function justificacionesFaltantes(
  alertas: readonly AlertaReceta[],
  justificaciones: Readonly<Record<string, string>>,
): AlertaReceta[] {
  return alertas.filter(
    (a) =>
      a.requiereJustificacion &&
      (justificaciones[a.clave] ?? '').trim().length < a.minimoJustificacion,
  );
}

export function dosisDiariaMg(
  dosisUnitariaMg: number | null | undefined,
  vecesPorDia: number | null | undefined,
): number | null {
  if (!dosisUnitariaMg || !vecesPorDia || dosisUnitariaMg <= 0 || vecesPorDia <= 0) {
    return null;
  }
  return Math.round(dosisUnitariaMg * vecesPorDia * 100) / 100;
}

export function diasRestantes(vigenteHasta: string, ahora: Date = new Date()): number {
  const diferencia = new Date(vigenteHasta).getTime() - ahora.getTime();
  return Math.max(0, Math.ceil(diferencia / 86_400_000));
}

export function textoVigencia(
  vigente: boolean,
  vigenteHasta: string,
  ahora: Date = new Date(),
): string {
  if (!vigente) {
    return new Date(vigenteHasta).getTime() < ahora.getTime() ? 'Vencida' : 'No vigente';
  }
  const dias = diasRestantes(vigenteHasta, ahora);
  return dias <= 1 ? 'Vence hoy o mañana' : `Vigente ${dias} días más`;
}

export function cantidadPendiente(receta: Pick<RecetaFarmacia, 'items'>): number {
  return receta.items.reduce((total, item) => total + item.pendiente, 0);
}
