export function severidadAusencia(
  estado: string,
): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
  switch (estado) {
    case 'APROBADA':
      return 'success';
    case 'SOLICITADA':
      return 'warn';
    case 'RECHAZADA':
      return 'danger';
    default:
      return 'secondary';
  }
}

export function severidadEmpleado(
  estado: string,
): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
  switch (estado) {
    case 'ACTIVO':
      return 'success';
    case 'LICENCIA':
      return 'warn';
    default:
      return 'secondary';
  }
}

export function lunesDe(fecha: Date): Date {
  const copia = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  const dia = copia.getDay();
  copia.setDate(copia.getDate() - (dia === 0 ? 6 : dia - 1));
  return copia;
}

export function sumarDias(fecha: Date, dias: number): Date {
  const copia = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  copia.setDate(copia.getDate() + dias);
  return copia;
}

export function horaCorta(hora: string): string {
  return hora.slice(0, 5);
}
