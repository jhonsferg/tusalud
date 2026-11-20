export type SuperficieDental = 'V' | 'L' | 'M' | 'D' | 'O';
export type EstadoPieza =
  | 'SANO'
  | 'CARIES'
  | 'OBTURADO'
  | 'SELLANTE'
  | 'FRACTURA'
  | 'CORONA'
  | 'ENDODONCIA'
  | 'IMPLANTE'
  | 'PROTESIS'
  | 'EXTRACCION_INDICADA'
  | 'AUSENTE';
export type EstadoPlanOdontologico =
  'BORRADOR' | 'PRESENTADO' | 'ACEPTADO' | 'RECHAZADO' | 'EN_CURSO' | 'COMPLETADO' | 'CANCELADO';
export type EstadoItemPlan = 'PENDIENTE' | 'REALIZADO' | 'CANCELADO';

export interface ProcedimientoOdontologico {
  id: string;
  codigo: string;
  nombre: string;
  categoria: string;
  precio: number;
  requierePieza: boolean;
  requiereSuperficie: boolean;
  estadoResultante: EstadoPieza | null;
  requiereConsentimiento: boolean;
  activo: boolean;
}

export type ProcedimientoOdontologicoRequest = Omit<ProcedimientoOdontologico, 'id'>;

export interface PiezaOdontograma {
  pieza: number;
  dentadura: 'PERMANENTE' | 'TEMPORAL';
  estado: EstadoPieza | null;
  observacion: string | null;
  superficies: Partial<Record<SuperficieDental, EstadoPieza>>;
  ultimoCambio: string | null;
}

export interface Odontograma {
  pacienteId: string;
  pacienteNombre: string;
  edadAnios: number | null;
  piezas: PiezaOdontograma[];
}

export interface CambioOdontograma {
  id: string;
  pieza: number;
  superficie: SuperficieDental | null;
  estado: EstadoPieza;
  observacion: string | null;
  registradoPor: string;
  registradoEn: string;
  itemId: string | null;
}

export interface CambioPiezaRequest {
  pieza: number;
  superficie?: SuperficieDental | null;
  estado: EstadoPieza;
  observacion?: string | null;
}

export interface ItemPlanOdontologico {
  id: string;
  procedimientoId: string;
  codigo: string;
  nombre: string;
  pieza: number | null;
  superficie: SuperficieDental | null;
  precio: number;
  estado: EstadoItemPlan;
  requiereConsentimiento: boolean;
  consentimientoId: string | null;
  realizadoEn: string | null;
  observacion: string | null;
}

export interface SesionOdontologica {
  id: string;
  fecha: string;
  medicoNombre: string;
  citaId: string | null;
  notas: string | null;
  itemIds: string[];
}

export interface PagoPlanOdontologico {
  id: string;
  monto: number;
  metodo: string;
  referencia: string | null;
  registradoPor: string;
  registradoEn: string;
}

export interface PlanOdontologicoResumen {
  id: string;
  pacienteId: string;
  pacienteNombre: string;
  medicoNombre: string;
  estado: EstadoPlanOdontologico;
  total: number;
  pagado: number;
  saldo: number;
  creadoEn: string;
}

export interface PlanOdontologico extends PlanOdontologicoResumen {
  notas: string | null;
  presentadoEn: string | null;
  resueltoEn: string | null;
  motivoRechazo: string | null;
  completadoEn: string | null;
  items: ItemPlanOdontologico[];
  sesiones: SesionOdontologica[];
  pagos: PagoPlanOdontologico[];
}

export interface ItemPlanRequest {
  procedimientoId: string;
  pieza?: number | null;
  superficie?: SuperficieDental | null;
  consentimientoId?: string | null;
  observacion?: string | null;
}

export interface SesionOdontologicaRequest {
  itemIds: string[];
  citaId?: string | null;
  notas?: string | null;
}

export interface PagoPlanRequest {
  monto: number;
  metodo: string;
  referencia?: string | null;
}
