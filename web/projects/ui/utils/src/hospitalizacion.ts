export const COLOR_TRIAJE: Readonly<Record<string, string>> = {
  ROJO: '#dc2626',
  NARANJA: '#ea580c',
  AMARILLO: '#ca8a04',
  VERDE: '#16a34a',
  AZUL: '#2563eb',
};

export const NIVELES_TRIAJE: readonly { nivel: number; clasificacion: string; nombre: string }[] = [
  { nivel: 1, clasificacion: 'ROJO', nombre: 'Resucitación' },
  { nivel: 2, clasificacion: 'NARANJA', nombre: 'Emergencia' },
  { nivel: 3, clasificacion: 'AMARILLO', nombre: 'Urgencia' },
  { nivel: 4, clasificacion: 'VERDE', nombre: 'Urgencia menor' },
  { nivel: 5, clasificacion: 'AZUL', nombre: 'No urgente' },
];

export function colorCama(estado: string): string {
  switch (estado) {
    case 'LIBRE':
      return 'var(--p-green-500)';
    case 'OCUPADA':
      return 'var(--p-red-500)';
    case 'LIMPIEZA':
      return 'var(--p-orange-500)';
    default:
      return 'var(--p-surface-500)';
  }
}
