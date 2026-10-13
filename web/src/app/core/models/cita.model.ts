export type EstadoCita = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'ATENDIDA';

export const ESTADOS_CITA: readonly EstadoCita[] = [
  'PENDIENTE',
  'CONFIRMADA',
  'ATENDIDA',
  'CANCELADA',
];

export const ESTADOS_CITA_ACTIVOS: readonly EstadoCita[] = ['PENDIENTE', 'CONFIRMADA'];

export interface CitaResponse {
  id: number;
  fechaHora: string;
  motivo: string;
  estado: EstadoCita;
  monto: number;
  medicoId: number;
  medicoNombre: string;
  especialidad: string;
  consultorio?: string | null;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDni: string;
  createdAt: string;
}

export interface CitaCreateRequest {
  medicoId: number;
  fechaHora: string;
  motivo: string;
}

export interface CitasPorEstadoResponse {
  estado: EstadoCita;
  cantidad: number;
}

export interface RangoFechas {
  inicio?: string | null;
  fin?: string | null;
}
