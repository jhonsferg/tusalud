import { PrioridadOrden } from './laboratorio.model';

export type ModalidadImagen = 'RX' | 'ECO' | 'TC' | 'RM';
export type EstadoOrdenImagen =
  'SOLICITADA' | 'PROGRAMADA' | 'REALIZADA' | 'INFORMADA' | 'CANCELADA';

export const MODALIDADES_IMAGEN: readonly ModalidadImagen[] = ['RX', 'ECO', 'TC', 'RM'];

export const ETIQUETA_MODALIDAD: Readonly<Record<ModalidadImagen, string>> = {
  RX: 'Radiografía',
  ECO: 'Ecografía',
  TC: 'Tomografía',
  RM: 'Resonancia',
};

export const ICONO_MODALIDAD: Readonly<Record<ModalidadImagen, string>> = {
  RX: 'pi-image',
  ECO: 'pi-wave-pulse',
  TC: 'pi-circle',
  RM: 'pi-bullseye',
};

export const ETIQUETA_ESTADO_IMAGEN: Readonly<Record<EstadoOrdenImagen, string>> = {
  SOLICITADA: 'Solicitada',
  PROGRAMADA: 'Programada',
  REALIZADA: 'Por informar',
  INFORMADA: 'Informada',
  CANCELADA: 'Cancelada',
};

export const ESTADOS_IMAGEN: readonly EstadoOrdenImagen[] = [
  'SOLICITADA',
  'PROGRAMADA',
  'REALIZADA',
  'INFORMADA',
  'CANCELADA',
];

export const SALAS_IMAGEN: readonly string[] = [
  'Sala de rayos X 1',
  'Sala de ecografía 1',
  'Sala de ecografía 2',
  'Sala de tomografía',
  'Sala de resonancia',
];

export interface EstudioImagenRequest {
  codigo: string;
  nombre: string;
  modalidad: ModalidadImagen;
  region: string;
  preparacion?: string | null;
  precio: number;
  duracionMinutos: number;
  activo?: boolean;
}

export interface EstudioImagenResponse {
  id: string;
  codigo: string;
  nombre: string;
  modalidad: ModalidadImagen;
  region: string;
  preparacion?: string | null;
  precio: number;
  duracionMinutos: number;
  activo: boolean;
}

export interface EstudioItemResponse {
  id: string;
  codigo: string;
  nombre: string;
  modalidad: ModalidadImagen;
  region: string;
  preparacion?: string | null;
  duracionMinutos: number;
}

export interface ArchivoImagenResponse {
  id: string;
  nombre: string;
  tipoContenido: string;
  tamano: number;
  subidoEn: string;
}

export interface InformeImagenResponse {
  id: string;
  version: number;
  hallazgos: string;
  conclusion: string;
  hallazgoCritico?: boolean | null;
  firmaNombre: string;
  firmaColegiatura: string;
  motivoCorreccion?: string | null;
  fecha: string;
}

export interface OrdenImagenResponse {
  id: string;
  codigo: string;
  fecha: string;
  estado: EstadoOrdenImagen;
  prioridad: PrioridadOrden;
  pacienteId?: string;
  pacienteNombre?: string;
  dni?: string | null;
  medicoId?: string | null;
  medicoNombre?: string | null;
  atencionId?: string | null;
  indicacionClinica?: string | null;
  total?: number | null;
  programadaPara?: string | null;
  sala?: string | null;
  realizadaEn?: string | null;
  realizadaPor?: string | null;
  observacionesToma?: string | null;
  informadaEn?: string | null;
  hallazgoCritico?: boolean | null;
  motivoCancelacion?: string | null;
  estudios: EstudioItemResponse[];
  archivos: ArchivoImagenResponse[];
  informe?: InformeImagenResponse | null;
}

export interface OrdenImagenRequest {
  pacienteId: string;
  atencionId?: string | null;
  prioridad: PrioridadOrden;
  indicacionClinica: string;
  estudioIds: string[];
}

export interface ProgramarImagenRequest {
  programadaPara: string;
  sala: string;
}

export interface RealizarImagenRequest {
  observaciones?: string | null;
}

export interface InformeImagenRequest {
  hallazgos: string;
  conclusion: string;
  hallazgoCritico: boolean;
  firmaColegiatura: string;
  motivoCorreccion?: string | null;
}

export interface AgendaImagenResponse {
  id: string;
  codigo: string;
  estado: EstadoOrdenImagen;
  prioridad: PrioridadOrden;
  programadaPara: string;
  duracionMinutos: number;
  sala: string;
  pacienteNombre: string;
  modalidades: ModalidadImagen[];
}

export interface ResumenImagenesResponse {
  solicitadas: number;
  programadas: number;
  porInformar: number;
  informadasHoy: number;
  urgentes: number;
}

export interface MiFirmaImagenResponse {
  nombre: string;
  colegiatura?: string | null;
}

export interface FiltroOrdenesImagen {
  estado?: EstadoOrdenImagen | null;
  q?: string | null;
  prioridad?: PrioridadOrden | null;
}
