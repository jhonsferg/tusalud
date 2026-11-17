export type DiaSemana =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DIAS_SEMANA: readonly DiaSemana[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export const NOMBRE_DIA: Readonly<Record<DiaSemana, string>> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

import { EspecialidadResponse } from './especialidad.model';

export type EstadoUsuario = 'ACTIVO' | 'INACTIVO';

export interface MedicoResponse {
  id: string;
  usuarioId: string;
  nombre: string;
  apellido: string;
  nombreCompleto: string;
  email: string;
  telefono?: string | null;
  colegiatura: string;
  consultorio?: string | null;
  especialidades: EspecialidadResponse[];
  estado: EstadoUsuario;
}

export interface MedicoCreateRequest {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  telefono?: string | null;
  colegiatura: string;
  consultorio?: string | null;
  especialidadIds: string[];
}

export interface HorarioRequest {
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFin: string;
  duracionCitaMinutos: number;
}

export interface HorarioResponse extends HorarioRequest {
  id: string;
  medicoId: string;
}

export interface DisponibilidadResponse {
  medicoId: string;
  medicoNombre: string;
  fecha: string;
  horariosDisponibles: string[];
}

export interface DisponibilidadDiaResponse {
  fecha: string;
  horariosDisponibles: string[];
  totalHoras: number;
}
