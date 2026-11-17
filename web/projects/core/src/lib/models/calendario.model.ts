export type TipoEvento = 'TAREA' | 'REUNION' | 'RECORDATORIO' | 'PERSONAL' | 'BLOQUEO';
export type ColorEventoApi = 'AZUL' | 'VERDE' | 'ROJO' | 'NARANJA' | 'MORADO' | 'GRIS';

export const TIPOS_EVENTO: readonly TipoEvento[] = [
  'REUNION',
  'TAREA',
  'RECORDATORIO',
  'PERSONAL',
  'BLOQUEO',
];

export const COLORES_EVENTO: readonly ColorEventoApi[] = [
  'AZUL',
  'VERDE',
  'ROJO',
  'NARANJA',
  'MORADO',
  'GRIS',
];

export interface EventoCalendarioResponse {
  id: string;
  titulo: string;
  descripcion?: string | null;
  ubicacion?: string | null;
  tipo: TipoEvento;
  inicio: string;
  fin: string;
  todoElDia: boolean;
  color?: ColorEventoApi | null;
  completado: boolean;
  recordatorioMinutos?: number | null;
}

export interface EventoCalendarioRequest {
  titulo: string;
  descripcion?: string | null;
  ubicacion?: string | null;
  tipo: TipoEvento;
  inicio: string;
  fin: string;
  todoElDia: boolean;
  color?: ColorEventoApi | null;
  recordatorioMinutos?: number | null;
}

export interface MoverEventoRequest {
  inicio: string;
  fin: string;
  todoElDia?: boolean;
}
