import {
  ColorEventoApi,
  EventoCalendarioResponse,
  parseIsoDateTime,
  Rol,
  TipoEvento,
} from '@tusalud/core';
import { CalCategoria, CalEvento, ColorEvento } from '@tusalud/ui/calendario';

export const COLOR_API: Readonly<Record<ColorEventoApi, ColorEvento>> = {
  AZUL: 'info',
  VERDE: 'success',
  ROJO: 'danger',
  NARANJA: 'warn',
  MORADO: 'purple',
  GRIS: 'secondary',
};

export const COLOR_POR_TIPO: Readonly<Record<TipoEvento, ColorEvento>> = {
  REUNION: 'purple',
  TAREA: 'success',
  RECORDATORIO: 'warn',
  PERSONAL: 'info',
  BLOQUEO: 'danger',
};

export const ETIQUETA_TIPO: Readonly<Record<TipoEvento, string>> = {
  REUNION: 'Reunión',
  TAREA: 'Tarea',
  RECORDATORIO: 'Recordatorio',
  PERSONAL: 'Personal',
  BLOQUEO: 'Bloqueo de agenda',
};

export const ICONO_TIPO: Readonly<Record<TipoEvento, string>> = {
  REUNION: 'pi-users',
  TAREA: 'pi-check-square',
  RECORDATORIO: 'pi-bell',
  PERSONAL: 'pi-user',
  BLOQUEO: 'pi-ban',
};

const TIPOS_POR_ROL: Readonly<Record<Rol, readonly TipoEvento[]>> = {
  ROLE_PACIENTE: ['PERSONAL', 'RECORDATORIO'],
  ROLE_ADMIN: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL'],
  ROLE_MEDICO: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL', 'BLOQUEO'],
  ROLE_LABORATORIO: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL'],
  ROLE_FARMACIA: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL'],
  ROLE_ALMACEN: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL'],
  ROLE_RECEPCION: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL'],
  ROLE_CAJA: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL'],
  ROLE_ENFERMERIA: ['REUNION', 'TAREA', 'RECORDATORIO', 'PERSONAL'],
};

const ORDEN_TIPOS: readonly TipoEvento[] = [
  'REUNION',
  'TAREA',
  'RECORDATORIO',
  'PERSONAL',
  'BLOQUEO',
];

export function tiposPermitidos(roles: readonly Rol[]): TipoEvento[] {
  const permitidos = new Set(roles.flatMap((rol) => TIPOS_POR_ROL[rol] ?? []));
  const unicamentePaciente = roles.length === 1 && roles[0] === 'ROLE_PACIENTE';
  return unicamentePaciente
    ? [...TIPOS_POR_ROL.ROLE_PACIENTE]
    : ORDEN_TIPOS.filter((tipo) => permitidos.has(tipo));
}

export function categoriasPara(roles: readonly Rol[]): CalCategoria[] {
  const tipos = new Set<string>(tiposPermitidos(roles));
  const esSoloAdmin =
    roles.length > 0 && roles.every((rol) => rol !== 'ROLE_MEDICO' && rol !== 'ROLE_PACIENTE');
  const base = CATEGORIAS_BASE.filter((categoria) =>
    categoria.clave === 'CITA' ? !esSoloAdmin : tipos.has(categoria.clave),
  );
  return tipos.has('BLOQUEO') ? [...base, CATEGORIA_BLOQUEO] : base;
}

export const CATEGORIAS_BASE: readonly CalCategoria[] = [
  { clave: 'CITA', nombre: 'Citas médicas', color: 'info' },
  { clave: 'REUNION', nombre: 'Reuniones', color: 'purple' },
  { clave: 'TAREA', nombre: 'Tareas', color: 'success' },
  { clave: 'RECORDATORIO', nombre: 'Recordatorios', color: 'warn' },
  { clave: 'PERSONAL', nombre: 'Personal', color: 'secondary' },
];

export const CATEGORIA_BLOQUEO: CalCategoria = {
  clave: 'BLOQUEO',
  nombre: 'Bloqueos de agenda',
  color: 'danger',
};

export interface DatosEvento {
  readonly origen: 'evento';
  readonly evento: EventoCalendarioResponse;
}

export function eventoApiACalendario(evento: EventoCalendarioResponse): CalEvento | null {
  const inicio = parseIsoDateTime(evento.inicio);
  const fin = parseIsoDateTime(evento.fin);
  if (!inicio || !fin) {
    return null;
  }
  const datos: DatosEvento = { origen: 'evento', evento };
  return {
    id: `e-${evento.id}`,
    inicio,
    fin: fin.getTime() > inicio.getTime() ? fin : new Date(inicio.getTime() + 30 * 60_000),
    titulo: evento.titulo,
    detalle: evento.ubicacion ?? undefined,
    color: evento.color ? COLOR_API[evento.color] : COLOR_POR_TIPO[evento.tipo],
    categoria: evento.tipo,
    todoElDia: evento.todoElDia,
    completado: evento.completado,
    ignoraCruce: true,
    editable: true,
    datos,
  };
}
