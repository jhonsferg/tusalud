import { EstadoEvento, SeveridadEvento, TipoReporteSeguridad } from '@tusalud/core';

export const ETIQUETA_TIPO_EVENTO: Record<TipoReporteSeguridad, string> = {
  EVENTO_ADVERSO: 'Evento adverso',
  CASI_INCIDENTE: 'Casi incidente',
  INCIDENTE_SIN_DANO: 'Incidente sin daño',
};

export const ETIQUETA_SEVERIDAD: Record<SeveridadEvento, string> = {
  LEVE: 'Leve',
  MODERADO: 'Moderado',
  GRAVE: 'Grave',
  CENTINELA: 'Centinela',
};

export const ETIQUETA_ESTADO_EVENTO: Record<EstadoEvento, string> = {
  NOTIFICADO: 'Notificado',
  EN_ANALISIS: 'En análisis',
  EN_ACCION: 'En acción',
  CERRADO: 'Cerrado',
};

export function barraProporcional(valor: number, total: number): number {
  return total === 0 ? 0 : Math.round((valor / total) * 100);
}
