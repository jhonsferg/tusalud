export type SexoPaciente = 'M' | 'F';

export type TipoDiagnostico = 'PRESUNTIVO' | 'DEFINITIVO' | 'REPETIDO';

export const TIPOS_DIAGNOSTICO: readonly { value: TipoDiagnostico; label: string }[] = [
  { value: 'DEFINITIVO', label: 'Definitivo' },
  { value: 'PRESUNTIVO', label: 'Presuntivo' },
  { value: 'REPETIDO', label: 'Repetido' },
];

export const ETIQUETA_SEXO: Readonly<Record<SexoPaciente, string>> = {
  M: 'Masculino',
  F: 'Femenino',
};

export interface Cie10Response {
  codigo: string;
  descripcion: string;
  capitulo?: string | null;
  grupo?: string | null;
  sexo?: SexoPaciente | null;
  edadMinima?: number | null;
  edadMaxima?: number | null;
  muestra: boolean;
}

export interface Cie10FrecuenteResponse {
  codigo: string;
  descripcion: string;
  casos: number;
}

export interface DiagnosticoRequest {
  codigo: string;
  tipo: TipoDiagnostico;
  principal: boolean;
  observacion?: string | null;
}

export interface DiagnosticoResponse {
  codigo: string;
  descripcion: string;
  tipo: TipoDiagnostico;
  principal: boolean;
  observacion?: string | null;
}

export interface Cie10VersionResponse {
  id: string;
  etiqueta: string;
  origen: 'MUESTRA' | 'IMPORTACION';
  total: number;
  altas: number;
  cambios: number;
  creadaPor?: string | null;
  creadaEn: string;
  activa: boolean;
}

export interface Cie10ResumenResponse {
  totalActivos: number;
  totalMuestra: number;
  soloMuestra: boolean;
  versionVigente?: Cie10VersionResponse | null;
  versiones: Cie10VersionResponse[];
}

export interface Cie10ImportacionError {
  fila: number;
  mensaje: string;
}

export interface Cie10ImportacionFila {
  codigo: string;
  descripcion: string;
  capitulo?: string | null;
  grupo?: string | null;
  sexo?: string | null;
  edadMinima?: number | null;
  edadMaxima?: number | null;
  accion: 'ALTA' | 'CAMBIO' | 'SIN_CAMBIO';
}

export interface Cie10ImportacionResponse {
  aplicada: boolean;
  filas: number;
  validas: number;
  altas: number;
  cambios: number;
  sinCambios: number;
  desactivados: number;
  totalErrores: number;
  errores: Cie10ImportacionError[];
  muestra: Cie10ImportacionFila[];
}

export interface MorbilidadDiagnostico {
  codigo: string;
  descripcion: string;
  capitulo: string;
  casos: number;
  porcentaje: number;
}

export interface MorbilidadCapitulo {
  capitulo: string;
  casos: number;
  porcentaje: number;
}

export interface MorbilidadDiagnosticoCorto {
  codigo: string;
  descripcion: string;
  casos: number;
}

export interface MorbilidadEspecialidad {
  especialidad: string;
  casos: number;
  top: MorbilidadDiagnosticoCorto[];
}

export interface MorbilidadGrupoEtario {
  grupo: string;
  masculino?: number | null;
  femenino?: number | null;
  sinDato?: number | null;
  total?: number | null;
  suprimido: boolean;
}

export interface MorbilidadPunto {
  fecha: string;
  casos: number;
}

export interface MorbilidadTendencia {
  codigo: string;
  descripcion: string;
  serie: MorbilidadPunto[];
}

export interface ReporteMorbilidadResponse {
  periodo: { desde: string; hasta: string; agrupacion: 'DIA' | 'SEMANA' | 'MES'; dias: number };
  casos: number;
  diagnosticosDistintos: number;
  umbralSupresion: number;
  top: MorbilidadDiagnostico[];
  porCapitulo: MorbilidadCapitulo[];
  porEspecialidad: MorbilidadEspecialidad[];
  porGrupoEtario: MorbilidadGrupoEtario[];
  serie: MorbilidadPunto[];
  tendencia: MorbilidadTendencia[];
}
