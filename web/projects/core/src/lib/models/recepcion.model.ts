export type PrioridadTurno = 'NORMAL' | 'PREFERENTE' | 'URGENTE';
export type EstadoTurno =
  'EN_ESPERA' | 'LLAMADO' | 'EN_ATENCION' | 'ATENDIDO' | 'AUSENTE' | 'CANCELADO';

export const ETIQUETA_PRIORIDAD: Readonly<Record<string, string>> = {
  NORMAL: 'Normal',
  PREFERENTE: 'Preferente',
  URGENTE: 'Urgente',
};

export const ETIQUETA_ESTADO_TURNO: Readonly<Record<string, string>> = {
  EN_ESPERA: 'En espera',
  LLAMADO: 'Llamado',
  EN_ATENCION: 'En atención',
  ATENDIDO: 'Atendido',
  AUSENTE: 'Ausente',
  CANCELADO: 'Cancelado',
};

export interface TurnoResponse {
  id: string;
  numero: string;
  fecha: string;
  pacienteId: string;
  pacienteNombre: string;
  dni: string;
  citaId?: string | null;
  medicoId?: string | null;
  medicoNombre?: string | null;
  prioridad: PrioridadTurno;
  estado: EstadoTurno;
  motivo?: string | null;
  consultorio?: string | null;
  creadoEn: string;
  llamadoEn?: string | null;
  inicioAtencionEn?: string | null;
  finAtencionEn?: string | null;
  esperaMinutos: number;
}

export interface TurnoRequest {
  pacienteId: string;
  medicoId?: string | null;
  prioridad: PrioridadTurno;
  motivo?: string | null;
}

export interface CitaHoyResponse {
  id: string;
  fechaHora: string;
  estado: string;
  pacienteId: string;
  pacienteNombre: string;
  pacienteDni: string;
  medicoNombre: string;
  especialidadNombre?: string | null;
  llego: boolean;
  turnoNumero?: string | null;
}

export interface PantallaTurno {
  numero: string;
  consultorio?: string | null;
  estado: EstadoTurno;
}

export interface ResumenRecepcionResponse {
  enEspera: number;
  llamados: number;
  atendidosHoy: number;
  ausentesHoy: number;
  esperaPromedioMinutos: number;
  citasPendientesDeLlegada: number;
}

export interface PacienteMostradorResponse {
  pacienteId: string;
  nombreCompleto: string;
  dni: string;
  telefono?: string | null;
  email?: string | null;
  fechaNacimiento?: string | null;
  historialClinicoNumero?: string | null;
}

export interface PacienteMostradorRequest {
  nombre: string;
  apellido: string;
  dni: string;
  fechaNacimiento: string;
  sexo?: string | null;
  telefono?: string | null;
  email?: string | null;
}

export interface PacienteRegistradoResponse {
  paciente: PacienteMostradorResponse;
  passwordTemporal: string;
}

export interface CitaMostradorRequest {
  pacienteId: string;
  medicoId: string;
  especialidadId: string;
  fechaHora: string;
  motivo: string;
}
