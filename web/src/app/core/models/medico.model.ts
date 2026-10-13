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

export type EstadoUsuario = 'ACTIVO' | 'INACTIVO';

export interface MedicoResponse {
  id: number;
  usuarioId: number;
  nombre: string;
  apellido: string;
  nombreCompleto: string;
  email: string;
  telefono?: string | null;
  colegiatura: string;
  consultorio?: string | null;
  precioConsulta: number;
  especialidadId: number;
  especialidadNombre: string;
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
  precioConsulta: number;
  especialidadId: number;
}

export interface HorarioRequest {
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFin: string;
  duracionCitaMinutos: number;
}

export interface HorarioResponse extends HorarioRequest {
  id: number;
  medicoId: number;
}

export interface DisponibilidadResponse {
  medicoId: number;
  medicoNombre: string;
  fecha: string;
  horariosDisponibles: string[];
}
