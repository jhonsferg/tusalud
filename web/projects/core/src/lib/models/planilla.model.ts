export type RegimenLaboral = 'GENERAL' | 'PEQUENA_EMPRESA' | 'MICROEMPRESA' | 'PRACTICAS';
export type SistemaPensionario = 'ONP' | 'AFP';
export type EstadoContrato = 'VIGENTE' | 'FINALIZADO';
export type TipoConceptoPlanilla = 'INGRESO' | 'DESCUENTO' | 'APORTE_EMPLEADOR';
export type ModoConcepto = 'MONTO_FIJO' | 'PORCENTAJE_BASICO';
export type TipoMarcacion = 'ENTRADA' | 'SALIDA';
export type OrigenMarcacion = 'MANUAL' | 'TERMINAL';
export type EstadoAsistencia =
  'PRESENTE' | 'TARDANZA' | 'FALTA' | 'AUSENCIA_APROBADA' | 'SIN_SALIDA' | 'SIN_TURNO';
export type DestinoExtra = 'PAGO' | 'COMPENSACION';
export type EstadoPlanilla = 'BORRADOR' | 'CALCULADA' | 'APROBADA' | 'PAGADA' | 'CERRADA';

export const ETIQUETA_REGIMEN: Readonly<Record<RegimenLaboral, string>> = {
  GENERAL: 'Régimen general',
  PEQUENA_EMPRESA: 'Pequeña empresa',
  MICROEMPRESA: 'Microempresa',
  PRACTICAS: 'Prácticas',
};

export const ETIQUETA_ESTADO_PLANILLA: Readonly<Record<EstadoPlanilla, string>> = {
  BORRADOR: 'Borrador',
  CALCULADA: 'Calculada',
  APROBADA: 'Aprobada',
  PAGADA: 'Pagada',
  CERRADA: 'Cerrada',
};

export const ETIQUETA_ESTADO_ASISTENCIA: Readonly<Record<EstadoAsistencia, string>> = {
  PRESENTE: 'Presente',
  TARDANZA: 'Tardanza',
  FALTA: 'Falta',
  AUSENCIA_APROBADA: 'Ausencia aprobada',
  SIN_SALIDA: 'Sin salida',
  SIN_TURNO: 'Sin turno',
};

export const ETIQUETA_TIPO_CONCEPTO: Readonly<Record<TipoConceptoPlanilla, string>> = {
  INGRESO: 'Ingreso',
  DESCUENTO: 'Descuento',
  APORTE_EMPLEADOR: 'Aporte del empleador',
};

export interface ContratoLaboralResponse {
  id: string;
  empleadoId: string;
  empleadoNombre: string;
  area: string;
  cargo: string;
  regimen: RegimenLaboral;
  remuneracion: number;
  asignacionFamiliar: boolean;
  sistemaPensionario: SistemaPensionario;
  afpNombre?: string | null;
  horasSemanales: number;
  banco?: string | null;
  cuentaEnmascarada?: string | null;
  fechaInicio: string;
  fechaFin?: string | null;
  estado: EstadoContrato;
  codigoMarcacion?: string | null;
}

export interface ContratoLaboralRequest {
  empleadoId: string;
  regimen: RegimenLaboral;
  remuneracion: number;
  asignacionFamiliar: boolean;
  sistemaPensionario: SistemaPensionario;
  afpNombre?: string | null;
  horasSemanales?: number | null;
  banco?: string | null;
  cuenta?: string | null;
  cargo: string;
  fechaInicio: string;
  fechaFin?: string | null;
}

export interface ParametroPlanillaResponse {
  id: string;
  codigo: string;
  valor: number;
  vigenteDesde: string;
  vigenteHasta?: string | null;
  descripcion: string;
  porValidar: boolean;
  vigente: boolean;
}

export interface ParametroPlanillaRequest {
  codigo: string;
  valor: number;
  vigenteDesde: string;
  descripcion?: string | null;
}

export interface ConceptoPlanillaResponse {
  id: string;
  codigo: string;
  nombre: string;
  tipo: TipoConceptoPlanilla;
  modo: ModoConcepto;
  valorDefecto: number;
  afectoRenta: boolean;
  afectoPension: boolean;
  afectoEssalud: boolean;
  activo: boolean;
  sistema: boolean;
}

export interface ConceptoPlanillaRequest {
  codigo: string;
  nombre: string;
  tipo: TipoConceptoPlanilla;
  modo: ModoConcepto;
  valorDefecto: number;
  afectoRenta: boolean;
  afectoPension: boolean;
  afectoEssalud: boolean;
  activo: boolean;
}

export interface EmpleadoConceptoResponse {
  id: string;
  conceptoId: string;
  conceptoCodigo: string;
  conceptoNombre: string;
  tipo: TipoConceptoPlanilla;
  modo: ModoConcepto;
  valor: number;
  vigenteDesde: string;
  vigenteHasta?: string | null;
  activo: boolean;
}

export interface EmpleadoConceptoRequest {
  conceptoId: string;
  valor: number;
  vigenteDesde: string;
  vigenteHasta?: string | null;
}

export interface MarcacionResponse {
  id: string;
  empleadoId: string;
  empleadoNombre: string;
  tipo: TipoMarcacion;
  fechaHora: string;
  origen: OrigenMarcacion;
  observacion?: string | null;
}

export interface MarcacionManualRequest {
  empleadoId: string;
  tipo: TipoMarcacion;
  fechaHora: string;
  observacion: string;
}

export interface TerminalAsistenciaResponse {
  id: string;
  nombre: string;
  activo: boolean;
  ultimoUso?: string | null;
}

export interface TerminalCreadaResponse {
  id: string;
  nombre: string;
  token: string;
}

export interface AsistenciaDiaResponse {
  id: string;
  empleadoId: string;
  empleadoNombre: string;
  fecha: string;
  programadaInicio?: string | null;
  programadaFin?: string | null;
  entrada?: string | null;
  salida?: string | null;
  minutosTardanza: number;
  minutosTrabajados: number;
  minutosExtra: number;
  estado: EstadoAsistencia;
  extraDestino: DestinoExtra;
  faltaJustificada: boolean;
  justificacion?: string | null;
  ajustada: boolean;
}

export interface ResumenAsistenciaResponse {
  desde: string;
  hasta: string;
  diasPresente: number;
  diasTardanza: number;
  diasFalta: number;
  diasFaltaJustificada: number;
  diasAusenciaAprobada: number;
  minutosTardanza: number;
  minutosTrabajados: number;
  minutosExtra: number;
  codigoMarcacion?: string | null;
  dias: AsistenciaDiaResponse[];
}

export interface ConsolidacionResponse {
  empleados: number;
  dias: number;
  ajustadasOmitidas: number;
}

export interface AjusteAsistenciaRequest {
  faltaJustificada?: boolean | null;
  minutosTardanza?: number | null;
  minutosExtra?: number | null;
  extraDestino?: DestinoExtra | null;
  justificacion: string;
}

export interface LineaBoletaResponse {
  codigo: string;
  nombre: string;
  tipo: TipoConceptoPlanilla;
  base?: number | null;
  monto: number;
}

export interface BoletaResumenResponse {
  id: string;
  numero: string;
  empleadoId: string;
  empleadoNombre: string;
  cargo: string;
  totalIngresos: number;
  totalDescuentos: number;
  neto: number;
  banco?: string | null;
}

export interface BoletaPagoResponse {
  id: string;
  numero: string;
  planillaId: string;
  planillaCodigo: string;
  anio: number;
  mes: number;
  estadoPlanilla: EstadoPlanilla;
  empleadoId: string;
  empleadoNombre: string;
  cargo: string;
  regimen: RegimenLaboral;
  sistemaPensionario: SistemaPensionario;
  banco?: string | null;
  cuentaEnmascarada?: string | null;
  diasLaborados: number;
  diasFalta: number;
  diasNoRemunerados: number;
  minutosTardanza: number;
  minutosExtraPagados: number;
  remuneracionBasica: number;
  totalIngresos: number;
  totalDescuentos: number;
  rentaQuinta: number;
  pension: number;
  aporteEmpleador: number;
  neto: number;
  provisionGratificacion: number;
  provisionCts: number;
  provisionVacaciones: number;
  hash: string;
  lineas: LineaBoletaResponse[];
}

export interface MiBoletaResumenResponse {
  id: string;
  numero: string;
  planillaCodigo: string;
  anio: number;
  mes: number;
  totalIngresos: number;
  totalDescuentos: number;
  neto: number;
  estadoPlanilla: EstadoPlanilla;
}

export interface PlanillaResponse {
  id: string;
  codigo: string;
  anio: number;
  mes: number;
  estado: EstadoPlanilla;
  cantidadBoletas: number;
  totalIngresos: number;
  totalDescuentos: number;
  totalNeto: number;
  totalAporteEmpleador: number;
  totalRenta: number;
  totalPension: number;
  hash?: string | null;
  calculadaPor?: string | null;
  calculadaEn?: string | null;
  aprobadaPor?: string | null;
  aprobadaEn?: string | null;
  pagadaPor?: string | null;
  pagadaEn?: string | null;
  fechaPago?: string | null;
  referenciaPago?: string | null;
  cerradaPor?: string | null;
  cerradaEn?: string | null;
}

export interface PlanillaDetalleResponse {
  planilla: PlanillaResponse;
  boletas: BoletaResumenResponse[];
}

export interface ArchivoPlanillaResponse {
  nombre: string;
  contenido: string;
  filas: number;
  formato: string;
  porValidar: boolean;
}

export interface PlanillaAuditoriaResponse {
  id: string;
  fecha: string;
  usuario: string;
  entidad: string;
  entidadId?: string | null;
  accion: string;
  detalle?: string | null;
}

export interface ResumenPlanillaResponse {
  empleadosConContrato: number;
  masaSalarialMensual: number;
  ultimaPlanilla?: PlanillaResponse | null;
  planillasPendientesAprobacion: number;
  marcacionesHoy: number;
}
