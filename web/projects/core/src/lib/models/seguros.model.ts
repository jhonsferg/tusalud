export type TipoAseguradora = 'SIS' | 'ESSALUD' | 'EPS' | 'PRIVADO' | 'EMPRESA';

export type CategoriaServicio =
  'CONSULTA' | 'LABORATORIO' | 'IMAGEN' | 'HOSPITALIZACION' | 'FARMACIA' | 'PROCEDIMIENTO';

export type TipoCobertura = 'PORCENTAJE' | 'COPAGO_FIJO';

export type EstadoPoliza = 'ACTIVA' | 'VENCIDA' | 'SUSPENDIDA';

export type EstadoPreautorizacion = 'SOLICITADA' | 'APROBADA' | 'RECHAZADA' | 'VENCIDA';

export type EstadoCuentaAseguradora = 'PENDIENTE' | 'LIQUIDADA' | 'PAGADA' | 'REVERTIDA';

export type EstadoLiquidacion = 'BORRADOR' | 'ENVIADA' | 'PAGADA' | 'OBSERVADA';

export const TIPOS_ASEGURADORA: readonly TipoAseguradora[] = [
  'SIS',
  'ESSALUD',
  'EPS',
  'PRIVADO',
  'EMPRESA',
];

export const CATEGORIAS_SERVICIO: readonly CategoriaServicio[] = [
  'CONSULTA',
  'LABORATORIO',
  'IMAGEN',
  'HOSPITALIZACION',
  'FARMACIA',
  'PROCEDIMIENTO',
];

export const ETIQUETA_TIPO_ASEGURADORA: Readonly<Record<TipoAseguradora, string>> = {
  SIS: 'Seguro público SIS',
  ESSALUD: 'EsSalud',
  EPS: 'EPS',
  PRIVADO: 'Seguro privado',
  EMPRESA: 'Convenio con empresa',
};

export const ETIQUETA_CATEGORIA_SERVICIO: Readonly<Record<CategoriaServicio, string>> = {
  CONSULTA: 'Consultas',
  LABORATORIO: 'Laboratorio',
  IMAGEN: 'Imágenes diagnósticas',
  HOSPITALIZACION: 'Hospitalización',
  FARMACIA: 'Farmacia',
  PROCEDIMIENTO: 'Procedimientos',
};

export const ETIQUETA_ESTADO_POLIZA: Readonly<Record<EstadoPoliza, string>> = {
  ACTIVA: 'Activa',
  VENCIDA: 'Vencida',
  SUSPENDIDA: 'Suspendida',
};

export const ETIQUETA_ESTADO_PREAUTORIZACION: Readonly<Record<EstadoPreautorizacion, string>> = {
  SOLICITADA: 'Solicitada',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  VENCIDA: 'Vencida',
};

export const ETIQUETA_ESTADO_LIQUIDACION: Readonly<Record<EstadoLiquidacion, string>> = {
  BORRADOR: 'Borrador',
  ENVIADA: 'Enviada',
  PAGADA: 'Pagada',
  OBSERVADA: 'Observada',
};

export const ETIQUETA_ESTADO_CUENTA: Readonly<Record<EstadoCuentaAseguradora, string>> = {
  PENDIENTE: 'Pendiente',
  LIQUIDADA: 'En liquidación',
  PAGADA: 'Pagada',
  REVERTIDA: 'Revertida',
};

export interface AseguradoraRequest {
  codigo: string;
  nombre: string;
  tipo: TipoAseguradora;
  ruc?: string | null;
  contacto?: string | null;
  activa?: boolean;
}

export interface AseguradoraResponse {
  id: string;
  codigo: string;
  nombre: string;
  tipo: TipoAseguradora;
  ruc?: string | null;
  contacto?: string | null;
  activa: boolean;
}

export interface ReglaCoberturaRequest {
  categoria: CategoriaServicio;
  tipo: TipoCobertura;
  valor: number;
  topePorServicio?: number | null;
  requierePreautorizacion: boolean;
}

export type ReglaCoberturaResponse = ReglaCoberturaRequest;

export interface ConvenioRequest {
  aseguradoraId: string;
  nombre: string;
  vigenciaDesde: string;
  vigenciaHasta: string;
  topeAnual?: number | null;
  activo: boolean;
  reglas: ReglaCoberturaRequest[];
  exclusiones: string[];
}

export interface ConvenioResponse {
  id: string;
  aseguradoraId: string;
  aseguradoraCodigo: string;
  aseguradoraNombre: string;
  nombre: string;
  vigenciaDesde: string;
  vigenciaHasta: string;
  topeAnual?: number | null;
  activo: boolean;
  vigente: boolean;
  reglas: ReglaCoberturaResponse[];
  exclusiones: string[];
}

export interface PolizaRequest {
  pacienteId: string;
  convenioId: string;
  numero: string;
  titularNombre: string;
  parentesco: string;
  plan?: string | null;
  vigenciaDesde: string;
  vigenciaHasta: string;
}

export interface PolizaEstadoRequest {
  estado: 'ACTIVA' | 'SUSPENDIDA';
  motivo?: string | null;
}

export interface PolizaResponse {
  id: string;
  pacienteId: string;
  pacienteNombre: string;
  pacienteDni: string;
  aseguradoraNombre: string;
  convenioId: string;
  convenioNombre: string;
  numero: string;
  titularNombre: string;
  parentesco: string;
  plan?: string | null;
  vigenciaDesde: string;
  vigenciaHasta: string;
  estado: EstadoPoliza;
  diasParaVencer?: number | null;
  motivoSuspension?: string | null;
}

export interface MiPolizaResponse {
  id: string;
  aseguradoraNombre: string;
  convenioNombre: string;
  numero: string;
  titularNombre: string;
  parentesco: string;
  plan?: string | null;
  vigenciaDesde: string;
  vigenciaHasta: string;
  estado: EstadoPoliza;
  diasParaVencer?: number | null;
  coberturas: ReglaCoberturaResponse[];
}

export interface VerificacionCoberturaResponse {
  polizaId: string;
  vigente: boolean;
  estado: string;
  saldoTope?: number | null;
  codigo: string;
  verificadaEn: string;
  mensaje: string;
}

export interface CoberturaPacienteResponse {
  pacienteId: string;
  tieneCobertura: boolean;
  aseguradora?: string | null;
  estado?: EstadoPoliza | null;
  requierenPreautorizacion: CategoriaServicio[];
}

export interface PreautorizacionRequest {
  pacienteId: string;
  categoria: CategoriaServicio;
  referenciaId?: string | null;
  descripcion: string;
  montoEstimado: number;
}

export interface AprobarPreautorizacionRequest {
  montoAprobado: number;
  vigenciaDias?: number | null;
}

export interface PreautorizacionResponse {
  id: string;
  codigo: string;
  pacienteId: string;
  pacienteNombre: string;
  aseguradoraNombre: string;
  polizaNumero: string;
  categoria: CategoriaServicio;
  referenciaId?: string | null;
  descripcion: string;
  montoEstimado: number;
  estado: EstadoPreautorizacion;
  solicitadaPorNombre: string;
  solicitadaEn: string;
  gestionadaPorNombre?: string | null;
  gestionadaEn?: string | null;
  codigoAutorizacion?: string | null;
  montoAprobado?: number | null;
  vigenteHasta?: string | null;
  motivoRechazo?: string | null;
}

export interface LineaDesgloseResponse {
  descripcion: string;
  monto: number;
  cubierto: number;
  copago: number;
  motivo?: string | null;
}

export interface DesgloseCobroResponse {
  origen: string;
  referenciaId?: string | null;
  total: number;
  tieneCobertura: boolean;
  aseguradora?: string | null;
  convenio?: string | null;
  polizaNumero?: string | null;
  montoCubierto: number;
  copago: number;
  saldoTopeAnual?: number | null;
  mensaje: string;
  lineas: LineaDesgloseResponse[];
}

export interface CuentaAseguradoraResponse {
  id: string;
  aseguradoraId: string;
  aseguradoraNombre: string;
  comprobanteCodigo?: string | null;
  categoria: CategoriaServicio;
  descripcion: string;
  montoTotal: number;
  montoCubierto: number;
  copago: number;
  fecha: string;
  estado: EstadoCuentaAseguradora;
  antiguedadDias: number;
  liquidacionCodigo?: string | null;
}

export interface CarteraResponse {
  aseguradoraId: string;
  aseguradoraNombre: string;
  pendiente: number;
  cuentas: number;
  tramo0a30: number;
  tramo31a60: number;
  tramo61a90: number;
  tramoMas90: number;
}

export interface LiquidacionCrearRequest {
  aseguradoraId: string;
  desde: string;
  hasta: string;
}

export interface LiquidacionPagoRequest {
  montoPagado: number;
  motivoGlosa?: string | null;
}

export interface LiquidacionResponse {
  id: string;
  codigo: string;
  aseguradoraId: string;
  aseguradoraNombre: string;
  desde: string;
  hasta: string;
  total: number;
  estado: EstadoLiquidacion;
  cantidadCuentas: number;
  creadaPorNombre: string;
  creadaEn: string;
  enviadaEn?: string | null;
  pagadaEn?: string | null;
  montoPagado?: number | null;
  montoGlosa?: number | null;
  motivoGlosa?: string | null;
}

export interface SegurosResumenResponse {
  carteraPendiente?: number | null;
  cuentasPendientes?: number | null;
  liquidacionesAbiertas?: number | null;
  polizasPorVencer?: number | null;
  preautorizacionesPendientes?: number | null;
}

export interface SegurosReporteResponse {
  totalCubierto: number;
  totalCopago: number;
  carteraPendiente: number;
  porAseguradora: {
    nombre: string;
    cuentas: number;
    cubierto: number;
    copago: number;
    pendiente: number;
  }[];
  antiguedad: { tramo: string; cuentas: number; monto: number }[];
}
