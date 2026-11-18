export function saludoDelDia(nombre: string, ahora = new Date()): string {
  const hora = ahora.getHours();
  const franja = hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches';
  const primero = nombre.trim().split(/\s+/)[0] ?? '';
  return primero ? `${franja}, ${primero}` : franja;
}

export function fechaLarga(ahora = new Date()): string {
  return ahora.toLocaleDateString('es-PE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

export function dec(valor: number | null | undefined): number {
  return Math.round((valor ?? 0) * 10) / 10;
}

export function fechaCorta(iso: string): string {
  const [anio, mes, dia] = iso.slice(0, 10).split('-').map(Number);
  if (!anio || !mes || !dia) {
    return iso;
  }
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-PE', {
    day: 'numeric',
    month: 'short',
  });
}

export function horaCorta(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  return new Date(iso).toLocaleTimeString('es-PE', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
}

export function nombreCorto(nombreCompleto: string): string {
  return nombreCompleto.trim().split(/\s+/)[0] ?? '';
}

export function hoyIso(ahora = new Date()): string {
  const mes = String(ahora.getMonth() + 1).padStart(2, '0');
  const dia = String(ahora.getDate()).padStart(2, '0');
  return `${ahora.getFullYear()}-${mes}-${dia}`;
}

export function inicioIso(diasAtras: number, ahora = new Date()): string {
  const fecha = new Date(ahora);
  fecha.setDate(fecha.getDate() - diasAtras);
  return hoyIso(fecha);
}
