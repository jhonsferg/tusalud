export const PALETA = [
  '#3b82f6',
  '#10b981',
  '#f59e0b',
  '#8b5cf6',
  '#ef4444',
  '#06b6d4',
  '#ec4899',
  '#64748b',
];

export const COLOR_TRIAJE_NIVEL: Readonly<Record<number, string>> = {
  1: '#dc2626',
  2: '#ea580c',
  3: '#ca8a04',
  4: '#16a34a',
  5: '#2563eb',
};

export function etiquetaFecha(fecha: string): string {
  return fecha.length >= 10 ? fecha.slice(5, 10).split('-').reverse().join('/') : fecha;
}

export function pct(valor: number | null | undefined): string {
  return (valor ?? 0).toFixed(1) + ' %';
}

export function horaDelDia(hora: number): string {
  return hora.toString().padStart(2, '0') + ':00';
}

export const OPCIONES_LINEA = {
  maintainAspectRatio: false,
  plugins: { legend: { position: 'bottom' } },
  scales: { y: { beginAtZero: true } },
  elements: { line: { tension: 0.3 } },
};

export const OPCIONES_BARRAS = {
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: { y: { beginAtZero: true } },
};

export const OPCIONES_BARRAS_LEYENDA = {
  maintainAspectRatio: false,
  plugins: { legend: { position: 'bottom' } },
  scales: { y: { beginAtZero: true } },
};

export const OPCIONES_APILADAS = {
  maintainAspectRatio: false,
  plugins: { legend: { position: 'bottom' } },
  scales: { x: { stacked: true }, y: { stacked: true, beginAtZero: true } },
};

export const OPCIONES_DONA = {
  maintainAspectRatio: false,
  cutout: '62%',
  plugins: { legend: { position: 'bottom' } },
};
