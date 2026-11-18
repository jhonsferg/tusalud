export type EstadoCita = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'ATENDIDA';

export const ESTADOS_CITA: readonly EstadoCita[] = [
  'PENDIENTE',
  'CONFIRMADA',
  'ATENDIDA',
  'CANCELADA',
];

export const ESTADOS_CITA_ACTIVOS: readonly EstadoCita[] = ['PENDIENTE', 'CONFIRMADA'];

export interface CitaResponse {
  id: string;
  fechaHora: string;
  motivo: string;
  estado: EstadoCita;
  monto: number;
  medicoId: string;
  medicoNombre: string;
  especialidad: string;
  especialidadId: string;
  especialidadNombre: string;
  precio: number;
  consultorio?: string | null;
  pacienteId: string;
  pacienteNombre: string;
  pacienteDni: string;
  createdAt: string;
  pagado?: boolean;
  duracionMinutos?: number;
  conflicto?: boolean;
  motivoCancelacion?: string | null;
}

export interface CitaCreateRequest {
  medicoId: string;
  especialidadId: string;
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
