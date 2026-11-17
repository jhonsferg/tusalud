export type AreaCama = 'HOSPITALIZACION' | 'EMERGENCIA' | 'UCI' | 'OBSERVACION';
export type EstadoCama = 'LIBRE' | 'OCUPADA' | 'MANTENIMIENTO' | 'LIMPIEZA';
export type TipoIngreso = 'PROGRAMADO' | 'EMERGENCIA';
export type EstadoIngreso = 'ACTIVO' | 'ALTA' | 'CANCELADO';
export type ClasificacionTriaje = 'ROJO' | 'NARANJA' | 'AMARILLO' | 'VERDE' | 'AZUL';

export const AREAS_CAMA: readonly AreaCama[] = [
  'HOSPITALIZACION',
  'EMERGENCIA',
  'UCI',
  'OBSERVACION',
];

export const ETIQUETA_AREA: Readonly<Record<string, string>> = {
  HOSPITALIZACION: 'Hospitalización',
  EMERGENCIA: 'Emergencia',
  UCI: 'UCI',
  OBSERVACION: 'Observación',
};

export const ETIQUETA_ESTADO_CAMA: Readonly<Record<string, string>> = {
  LIBRE: 'Libre',
  OCUPADA: 'Ocupada',
  MANTENIMIENTO: 'Mantenimiento',
  LIMPIEZA: 'Limpieza',
};

export const ETIQUETA_TRIAJE: Readonly<Record<number, string>> = {
  1: 'Resucitación',
  2: 'Emergencia',
  3: 'Urgencia',
  4: 'Urgencia menor',
  5: 'No urgente',
};

export interface CamaResponse {
  id: string;
  codigo: string;
  area: AreaCama;
  piso?: string | null;
  descripcion?: string | null;
  estado: EstadoCama;
  precioDia: number;
  activa: boolean;
  ingresoActivoId?: string | null;
  pacienteNombre?: string | null;
}

export interface CamaRequest {
  codigo: string;
  area: AreaCama;
  piso?: string | null;
  descripcion?: string | null;
  precioDia: number;
  activa: boolean;
}

export interface TriajeRequest {
  nivel: number;
  presionArterial?: string | null;
  frecuenciaCardiaca?: number | null;
  frecuenciaRespiratoria?: number | null;
  temperatura?: number | null;
  saturacionO2?: number | null;
  glasgow?: number | null;
  motivoConsulta: string;
}

export interface TriajeResponse extends TriajeRequest {
  clasificacion: ClasificacionTriaje;
  registradoPor: string;
  fecha: string;
}

export interface IngresoHospResponse {
  id: string;
  codigo: string;
  tipo: TipoIngreso;
  estado: EstadoIngreso;
  pacienteId: string;
  pacienteNombre: string;
  dni: string;
  camaId?: string | null;
  camaCodigo?: string | null;
  medicoId?: string | null;
  medicoNombre?: string | null;
  motivo: string;
  diagnosticoIngreso?: string | null;
  fechaIngreso: string;
  fechaAlta?: string | null;
  diagnosticoAlta?: string | null;
  indicacionesAlta?: string | null;
  diasEstancia: number;
  cargoCama: number;
  triaje?: TriajeResponse | null;
  esperaMinutos?: number | null;
}

export interface IngresoHospRequest {
  pacienteId: string;
  camaId: string;
  motivo: string;
  diagnosticoIngreso?: string | null;
  medicoId?: string | null;
}

export interface AltaRequest {
  diagnosticoAlta: string;
  indicacionesAlta: string;
}

export interface EmergenciaRequest {
  pacienteId: string;
  triaje: TriajeRequest;
}

export interface EvolucionRequest {
  tipo: 'MEDICA' | 'ENFERMERIA';
  nota: string;
  presionArterial?: string | null;
  frecuenciaCardiaca?: number | null;
  temperatura?: number | null;
  saturacionO2?: number | null;
}

export interface EvolucionResponse extends EvolucionRequest {
  id: string;
  fecha: string;
  autorNombre: string;
}

export interface ResumenHospResponse {
  camasLibres: number;
  camasOcupadas: number;
  camasLimpieza: number;
  ingresosActivos: number;
  emergenciasEnEspera: number;
  ocupacionPorcentaje: number;
}

export interface MapaCamasResponse {
  area: AreaCama;
  total?: number;
  libres?: number;
  ocupadas?: number;
  camas: CamaResponse[];
}
