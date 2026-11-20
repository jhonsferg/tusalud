import { toIsoDate } from '@tusalud/core';

export function inicioDeMes(fecha: Date = new Date()): Date {
  return new Date(fecha.getFullYear(), fecha.getMonth(), 1);
}

export function rangoDelMes(fecha: Date = new Date()): { desde: string; hasta: string } {
  return { desde: toIsoDate(inicioDeMes(fecha)), hasta: toIsoDate(fecha) };
}

export function redondear(valor: number): number {
  return Math.round((valor + Number.EPSILON) * 100) / 100;
}

export function descargarTexto(nombre: string, contenido: string): void {
  const blob = new Blob([contenido], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
}

export const MESES: { label: string; value: number }[] = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
].map((label, i) => ({ label, value: i + 1 }));

export function minutosAHoras(minutos: number): string {
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return `${h} h ${m.toString().padStart(2, '0')} min`;
}

export function etiquetaPeriodo(anio: number, mes: number): string {
  return `${MESES[mes - 1]?.label ?? mes} ${anio}`;
}
