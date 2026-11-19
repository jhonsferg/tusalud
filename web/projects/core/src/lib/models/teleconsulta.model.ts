export type ModalidadCita = 'PRESENCIAL' | 'VIRTUAL';

export type EstadoTeleconsulta =
  'PROGRAMADA' | 'SALA_ABIERTA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_ASISTIO';

export type RolTeleconsulta = 'MEDICO' | 'PACIENTE';

export const ETIQUETA_ESTADO_TELECONSULTA: Readonly<Record<EstadoTeleconsulta, string>> = {
  PROGRAMADA: 'Programada',
  SALA_ABIERTA: 'Sala abierta',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  NO_ASISTIO: 'No asistió',
};

export interface TeleconsultaResponse {
  id: string;
  citaId: string;
  fechaHora: string;
  duracionMinutos: number;
  estado: EstadoTeleconsulta;
  medicoNombre: string;
  especialidad: string;
  pacienteNombre: string;
  motivo: string;
  rol: RolTeleconsulta;
  ventanaDesde: string;
  ventanaHasta: string;
  puedeIngresar: boolean;
  salaAbierta: boolean;
  consentimientoPendiente: boolean;
  duracionSegundos?: number | null;
}

export interface ConsentimientoTelemedicinaResponse {
  version: string;
  texto: string;
  aceptado: boolean;
  aceptadoEn?: string | null;
}

export interface TicketTeleconsultaResponse {
  ticket: string;
  expiraEnSegundos: number;
  ruta: string;
}

export interface IceServer {
  urls: string[];
  username?: string;
  credential?: string;
}

export interface IceServersResponse {
  iceServers: IceServer[];
  expiraEpochSegundos: number;
}

export interface ResumenPacienteTeleconsultaResponse {
  pacienteNombre: string;
  edad?: number | null;
  grupoSanguineo?: string | null;
  alergias?: string | null;
  motivo: string;
}
