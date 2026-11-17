export type VistaCalendario = 'mes' | 'semana' | 'dia' | 'agenda';

export type ColorEvento = 'info' | 'success' | 'warn' | 'danger' | 'secondary' | 'purple';

export interface CalEvento {
  readonly id: string;
  readonly inicio: Date;
  readonly fin: Date;
  readonly titulo: string;
  readonly detalle?: string;
  readonly color: ColorEvento;
  readonly ignoraCruce?: boolean;
  readonly todoElDia?: boolean;
  readonly categoria?: string;
  readonly editable?: boolean;
  readonly completado?: boolean;
  readonly datos?: unknown;
}

export interface CalCategoria {
  readonly clave: string;
  readonly nombre: string;
  readonly color: ColorEvento;
}

export interface HorarioDia {
  readonly dia: number;
  readonly inicio: string;
  readonly fin: string;
}

export interface CrearEvento {
  readonly inicio: Date;
  readonly fin: Date;
  readonly todoElDia: boolean;
}

export interface CambioEvento {
  readonly evento: CalEvento;
  readonly inicio: Date;
  readonly fin: Date;
  readonly todoElDia: boolean;
}

export interface CalHueco {
  readonly inicio: Date;
  readonly fin: Date;
}

export interface RangoVisible {
  readonly inicio: Date;
  readonly fin: Date;
}

export interface EventoPosicionado {
  readonly evento: CalEvento;
  readonly columna: number;
  readonly columnas: number;
  readonly conflicto: boolean;
}

const MS_MINUTO = 60_000;
export const DIAS_AGENDA = 30;

export function inicioDelDia(fecha: Date): Date {
  return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
}

export function sumarDias(fecha: Date, dias: number): Date {
  return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate() + dias);
}

export function mismoDia(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function inicioDeSemana(fecha: Date): Date {
  const dia = (fecha.getDay() + 6) % 7;
  return sumarDias(inicioDelDia(fecha), -dia);
}

export function diasDeRango(inicio: Date, cantidad: number): Date[] {
  return Array.from({ length: cantidad }, (_, indice) => sumarDias(inicio, indice));
}

export function rangoVisible(vista: VistaCalendario, fecha: Date): RangoVisible {
  if (vista === 'dia') {
    const dia = inicioDelDia(fecha);
    return { inicio: dia, fin: dia };
  }
  if (vista === 'agenda') {
    const dia = inicioDelDia(fecha);
    return { inicio: dia, fin: sumarDias(dia, DIAS_AGENDA - 1) };
  }
  if (vista === 'semana') {
    const inicio = inicioDeSemana(fecha);
    return { inicio, fin: sumarDias(inicio, 6) };
  }
  const primero = new Date(fecha.getFullYear(), fecha.getMonth(), 1);
  const ultimo = new Date(fecha.getFullYear(), fecha.getMonth() + 1, 0);
  const inicio = inicioDeSemana(primero);
  const fin = sumarDias(inicioDeSemana(ultimo), 6);
  return { inicio, fin };
}

export function moverFecha(vista: VistaCalendario, fecha: Date, pasos: number): Date {
  if (vista === 'dia') {
    return sumarDias(fecha, pasos);
  }
  if (vista === 'agenda') {
    return sumarDias(fecha, pasos * DIAS_AGENDA);
  }
  if (vista === 'semana') {
    return sumarDias(fecha, pasos * 7);
  }
  return new Date(fecha.getFullYear(), fecha.getMonth() + pasos, 1);
}

export function solapan(a: { inicio: Date; fin: Date }, b: { inicio: Date; fin: Date }): boolean {
  return a.inicio.getTime() < b.fin.getTime() && b.inicio.getTime() < a.fin.getTime();
}

export function minutosDesde(fecha: Date, base: number): number {
  return fecha.getHours() * 60 + fecha.getMinutes() - base * 60;
}

export function duracionMinutos(evento: { inicio: Date; fin: Date }): number {
  return Math.max(1, Math.round((evento.fin.getTime() - evento.inicio.getTime()) / MS_MINUTO));
}

export function distribuir(eventos: readonly CalEvento[]): EventoPosicionado[] {
  const ordenados = [...eventos].sort(
    (a, b) => a.inicio.getTime() - b.inicio.getTime() || b.fin.getTime() - a.fin.getTime(),
  );
  const resultado: EventoPosicionado[] = [];
  let grupo: { evento: CalEvento; columna: number }[] = [];
  let finGrupo = 0;
  let columnasFin: number[] = [];

  const cerrar = () => {
    const columnas = Math.max(1, columnasFin.length);
    for (const item of grupo) {
      const conflicto =
        !item.evento.ignoraCruce &&
        grupo.some(
          (otro) =>
            otro.evento !== item.evento &&
            !otro.evento.ignoraCruce &&
            solapan(item.evento, otro.evento),
        );
      resultado.push({ evento: item.evento, columna: item.columna, columnas, conflicto });
    }
    grupo = [];
    columnasFin = [];
  };

  for (const evento of ordenados) {
    if (grupo.length > 0 && evento.inicio.getTime() >= finGrupo) {
      cerrar();
    }
    let columna = columnasFin.findIndex((fin) => fin <= evento.inicio.getTime());
    if (columna === -1) {
      columna = columnasFin.length;
      columnasFin.push(evento.fin.getTime());
    } else {
      columnasFin[columna] = evento.fin.getTime();
    }
    grupo.push({ evento, columna });
    finGrupo = Math.max(finGrupo, evento.fin.getTime());
  }
  if (grupo.length > 0) {
    cerrar();
  }
  return resultado;
}

export function contarCruces(eventos: readonly CalEvento[]): number {
  return distribuir(eventos).filter((item) => item.conflicto).length;
}

export function recortarAlDia(evento: CalEvento, dia: Date): CalEvento | null {
  const desde = inicioDelDia(dia);
  const hasta = sumarDias(desde, 1);
  if (evento.inicio >= hasta || evento.fin <= desde) {
    return null;
  }
  if (evento.inicio >= desde && evento.fin <= hasta) {
    return evento;
  }
  return {
    ...evento,
    inicio: evento.inicio < desde ? desde : evento.inicio,
    fin: evento.fin > hasta ? hasta : evento.fin,
  };
}

export function ajustarMinutos(minutos: number, paso: number): number {
  return Math.round(minutos / paso) * paso;
}

export function fechaConMinutos(dia: Date, minutos: number): Date {
  return new Date(dia.getFullYear(), dia.getMonth(), dia.getDate(), 0, minutos);
}

export function minutosDeHora(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + (m || 0);
}

export function numeroDeSemana(fecha: Date): number {
  const objetivo = new Date(Date.UTC(fecha.getFullYear(), fecha.getMonth(), fecha.getDate()));
  const dia = objetivo.getUTCDay() || 7;
  objetivo.setUTCDate(objetivo.getUTCDate() + 4 - dia);
  const inicioAnio = new Date(Date.UTC(objetivo.getUTCFullYear(), 0, 1));
  return Math.ceil(((objetivo.getTime() - inicioAnio.getTime()) / 86_400_000 + 1) / 7);
}
