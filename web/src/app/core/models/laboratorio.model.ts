export type CategoriaExamen =
  'HEMATOLOGIA' | 'BIOQUIMICA' | 'ORINA' | 'INMUNOLOGIA' | 'MICROBIOLOGIA' | 'OTROS';
export type TipoMuestra = 'SANGRE' | 'ORINA' | 'HECES' | 'SALIVA' | 'OTRA';
export type TipoParametro = 'NUMERICO' | 'TEXTO';
export type EstadoOrden =
  'SOLICITADA' | 'MUESTRA_TOMADA' | 'EN_PROCESO' | 'COMPLETADA' | 'CANCELADA';
export type EstadoItemOrden = 'PENDIENTE' | 'CON_RESULTADO' | 'VALIDADO';
export type PrioridadOrden = 'NORMAL' | 'URGENTE';

export const CATEGORIAS_EXAMEN: readonly CategoriaExamen[] = [
  'HEMATOLOGIA',
  'BIOQUIMICA',
  'ORINA',
  'INMUNOLOGIA',
  'MICROBIOLOGIA',
  'OTROS',
];

export const ETIQUETA_CATEGORIA: Readonly<Record<CategoriaExamen, string>> = {
  HEMATOLOGIA: 'Hematología',
  BIOQUIMICA: 'Bioquímica',
  ORINA: 'Orina',
  INMUNOLOGIA: 'Inmunología',
  MICROBIOLOGIA: 'Microbiología',
  OTROS: 'Otros',
};

export const TIPOS_MUESTRA: readonly TipoMuestra[] = ['SANGRE', 'ORINA', 'HECES', 'SALIVA', 'OTRA'];

export const ETIQUETA_MUESTRA: Readonly<Record<TipoMuestra, string>> = {
  SANGRE: 'Sangre',
  ORINA: 'Orina',
  HECES: 'Heces',
  SALIVA: 'Saliva',
  OTRA: 'Otra',
};

export const ETIQUETA_ESTADO_ORDEN: Readonly<Record<EstadoOrden, string>> = {
  SOLICITADA: 'Solicitada',
  MUESTRA_TOMADA: 'Muestra tomada',
  EN_PROCESO: 'En proceso',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
};

export interface ParametroExamen {
  id?: number;
  nombre: string;
  unidad?: string | null;
  tipo: TipoParametro;
  valorMin?: number | null;
  valorMax?: number | null;
  orden: number;
}

export interface ExamenResponse {
  id: number;
  codigo: string;
  nombre: string;
  categoria: CategoriaExamen;
  muestra: TipoMuestra;
  precio: number;
  tiempoEntregaHoras: number;
  indicaciones?: string | null;
  activo: boolean;
  parametros: ParametroExamen[];
}

export interface ExamenRequest {
  codigo: string;
  nombre: string;
  categoria: CategoriaExamen;
  muestra: TipoMuestra;
  precio: number;
  tiempoEntregaHoras: number;
  indicaciones?: string | null;
  activo: boolean;
  parametros: ParametroExamen[];
}

export interface ResultadoParametro {
  parametroId: number;
  parametro: string;
  unidad?: string | null;
  valor: string;
  valorMin?: number | null;
  valorMax?: number | null;
  fueraDeRango: boolean;
}

export interface ItemOrdenResponse {
  id: number;
  examenId: number;
  examenNombre: string;
  categoria: CategoriaExamen;
  muestra: TipoMuestra;
  estado: EstadoItemOrden;
  observaciones?: string | null;
  fechaResultado?: string | null;
  validadoPor?: string | null;
  resultados: ResultadoParametro[];
}

export interface OrdenResponse {
  id: number;
  codigo: string;
  fecha: string;
  estado: EstadoOrden;
  prioridad: PrioridadOrden;
  pacienteId: number;
  pacienteNombre: string;
  dni: string;
  medicoId: number;
  medicoNombre: string;
  atencionId?: number | null;
  indicaciones?: string | null;
  total: number;
  tomaMuestraEn?: string | null;
  completadaEn?: string | null;
  items: ItemOrdenResponse[];
}

export interface OrdenRequest {
  pacienteId: number;
  atencionId?: number | null;
  prioridad: PrioridadOrden;
  indicaciones?: string | null;
  examenIds: number[];
}

export interface ResultadosRequest {
  resultados: { parametroId: number; valor: string }[];
  observaciones?: string | null;
}

export interface ResumenLaboratorioResponse {
  pendientes: number;
  enProceso: number;
  completadasHoy: number;
  urgentes: number;
}

export interface FiltroOrdenes {
  estado?: EstadoOrden | null;
  q?: string | null;
  prioridad?: PrioridadOrden | null;
}
