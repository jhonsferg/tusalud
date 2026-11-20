export type EstadoQuirofano = 'DISPONIBLE' | 'MANTENIMIENTO' | 'FUERA_DE_SERVICIO';
export type TipoCirugia = 'ELECTIVA' | 'URGENCIA';
export type EstadoCirugia =
  | 'SOLICITADA'
  | 'PROGRAMADA'
  | 'EN_PREPARACION'
  | 'EN_CURSO'
  | 'FINALIZADA'
  | 'SUSPENDIDA'
  | 'CANCELADA';
export type TipoAnestesia = 'GENERAL' | 'REGIONAL' | 'LOCAL' | 'SEDACION' | 'NINGUNA';
export type RolQuirurgico =
  'CIRUJANO' | 'AYUDANTE' | 'ANESTESIOLOGO' | 'INSTRUMENTISTA' | 'CIRCULANTE';
export type MomentoChecklist = 'ANTES_INDUCCION' | 'ANTES_INCISION' | 'ANTES_SALIDA';
export type TipoEsterilizacion = 'VAPOR' | 'PLASMA' | 'OXIDO_ETILENO';
export type ResultadoIndicador = 'PENDIENTE' | 'CONFORME' | 'NO_CONFORME';
export type ResultadoCiclo = 'EN_PROCESO' | 'CONFORME' | 'NO_CONFORME';
export type EstadoEfectivoLote =
  'ESTERIL' | 'EN_ESPERA_DE_INDICADOR' | 'EN_USO' | 'USADO' | 'BLOQUEADO' | 'VENCIDO';
export type EstadoConsentimientoCirugia = 'PENDIENTE_FIRMA' | 'FIRMADO' | 'REVOCADO' | 'VENCIDO';

export interface EquipoQuirofano {
  id: string;
  nombre: string;
  serie?: string | null;
}

export interface Quirofano {
  id: string;
  codigo: string;
  nombre: string;
  ubicacion?: string | null;
  estado: EstadoQuirofano;
  activo: boolean;
  observaciones?: string | null;
  equipos: EquipoQuirofano[];
}

export interface QuirofanoRequest {
  codigo: string;
  nombre: string;
  ubicacion?: string | null;
  estado: EstadoQuirofano;
  activo: boolean;
  observaciones?: string | null;
  equipos: { nombre: string; serie?: string | null }[];
}

export interface MiembroEquipo {
  usuarioId: string;
  nombre: string;
  rol: RolQuirurgico;
}

export interface CirugiaResumen {
  id: string;
  codigo: string;
  estado: EstadoCirugia;
  tipo: TipoCirugia;
  quirofanoId?: string | null;
  quirofanoNombre?: string | null;
  inicio?: string | null;
  fin?: string | null;
  detalleVisible: boolean;
  pacienteId?: string | null;
  pacienteNombre?: string | null;
  procedimiento?: string | null;
  solicitanteNombre?: string | null;
  enMiEquipo: boolean;
}

export interface ChecklistCirugia {
  momento: MomentoChecklist;
  items: string[];
  observaciones?: string | null;
  completadoPor: string;
  completadoEn: string;
}

export interface InsumoCirugia {
  id: string;
  productoId: string;
  producto: string;
  unidad: string;
  cantidad: number;
  registradoPor: string;
  registradoEn: string;
}

export interface SetUsado {
  id: string;
  loteId: string;
  loteCodigo: string;
  set: string;
  cicloNumero: string;
  esterilizadoEn: string;
  venceEn: string;
  registradoPor: string;
  registradoEn: string;
}

export interface EventoCirugia {
  fecha: string;
  usuario: string;
  accion: string;
  detalle?: string | null;
}

export interface ConsentimientoCirugia {
  id: string;
  estado: EstadoConsentimientoCirugia;
  firmadoEn?: string | null;
}

export interface NotaOperatoria {
  hallazgos: string;
  tecnica: string;
  complicaciones?: string | null;
  sangradoMl?: number | null;
  diagnosticoPostoperatorio: string;
  indicacionesPostoperatorias?: string | null;
  registradaEn: string;
  registradaPor?: string | null;
}

export interface RegistroAnestesico {
  asa?: number | null;
  inicio: string;
  fin: string;
  observaciones?: string | null;
  registradoEn: string;
  registradoPor?: string | null;
}

export interface CirugiaDetalle {
  resumen: CirugiaResumen;
  diagnosticoCodigo?: string | null;
  diagnosticoDescripcion?: string | null;
  tipoAnestesia: TipoAnestesia;
  ingresoId?: string | null;
  ingresoCodigo?: string | null;
  indicacionesPreoperatorias?: string | null;
  motivo?: string | null;
  inicioReal?: string | null;
  finReal?: string | null;
  consentimientoQuirurgico?: ConsentimientoCirugia | null;
  consentimientoAnestesico?: ConsentimientoCirugia | null;
  equipo: MiembroEquipo[];
  checklists: ChecklistCirugia[];
  insumos: InsumoCirugia[];
  sets: SetUsado[];
  notaOperatoria?: NotaOperatoria | null;
  registroAnestesico?: RegistroAnestesico | null;
  eventos: EventoCirugia[];
  pendientesParaIniciar: string[];
  pendientesParaFinalizar: string[];
  clinicoVisible: boolean;
}

export interface MiCirugia {
  id: string;
  codigo: string;
  estado: EstadoCirugia;
  procedimiento: string;
  quirofano?: string | null;
  inicio?: string | null;
  cirujano: string;
  consentimientoQuirurgico?: ConsentimientoCirugia | null;
  consentimientoAnestesico?: ConsentimientoCirugia | null;
}

export interface SolicitudCirugiaRequest {
  pacienteId: string;
  diagnosticoCodigo: string;
  procedimiento: string;
  tipo: TipoCirugia;
  tipoAnestesia: TipoAnestesia;
  ingresoId?: string | null;
  indicacionesPreoperatorias?: string | null;
}

export interface ProgramacionRequest {
  quirofanoId: string;
  inicio: string;
  duracionMin: number;
  equipo: { usuarioId: string; rol: RolQuirurgico }[];
}

export interface ChecklistRequest {
  momento: MomentoChecklist;
  items: string[];
  observaciones?: string | null;
}

export interface NotaOperatoriaRequest {
  hallazgos: string;
  tecnica: string;
  complicaciones?: string | null;
  sangradoMl?: number | null;
  diagnosticoPostoperatorio: string;
  indicacionesPostoperatorias?: string | null;
}

export interface RegistroAnestesicoRequest {
  asa: number;
  inicio: string;
  fin: string;
  observaciones?: string | null;
}

export interface InsumoDisponible {
  id: string;
  codigo: string;
  nombre: string;
  unidad: string;
  stock: number;
}

export interface Esterilizador {
  id: string;
  codigo: string;
  nombre: string;
  tipo: TipoEsterilizacion;
  activo: boolean;
}

export interface EsterilizadorRequest {
  codigo: string;
  nombre: string;
  tipo: TipoEsterilizacion;
  activo: boolean;
}

export interface SetInstrumental {
  id: string;
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  vigenciaDias: number;
  activo: boolean;
}

export interface SetInstrumentalRequest {
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  vigenciaDias: number;
  activo: boolean;
}

export interface LoteEsteril {
  id: string;
  codigo: string;
  setId: string;
  setNombre: string;
  cicloId: string;
  cicloNumero: string;
  esterilizadoEn: string;
  venceEn: string;
  estado: EstadoEfectivoLote;
  utilizable: boolean;
}

export interface CicloEsterilizacion {
  id: string;
  numero: string;
  esterilizadorId: string;
  esterilizador: string;
  iniciadoEn: string;
  temperaturaC: number;
  presionKpa?: number | null;
  duracionMin: number;
  indicadorQuimico: ResultadoIndicador;
  indicadorBiologico: ResultadoIndicador;
  resultado: ResultadoCiclo;
  responsable: string;
  observaciones?: string | null;
  cerradoEn?: string | null;
  lotes: LoteEsteril[];
}

export interface CicloRequest {
  esterilizadorId: string;
  iniciadoEn: string;
  temperaturaC: number;
  presionKpa?: number | null;
  duracionMin: number;
  observaciones?: string | null;
  sets: { setId: string; cantidad: number }[];
}

export interface IndicadoresRequest {
  quimico: ResultadoIndicador;
  biologico: ResultadoIndicador;
  observaciones?: string | null;
}

export interface UsoLote {
  cirugiaCodigo: string;
  cirugiaId: string;
  estado: EstadoCirugia;
  fecha: string;
  quirofano?: string | null;
}

export interface TrazabilidadLote {
  lote: LoteEsteril;
  resultadoCiclo: ResultadoCiclo;
  usos: UsoLote[];
}

export interface AlertaEsterilizacion {
  tipo: string;
  severidad: 'CRITICA' | 'ALTA' | 'MEDIA' | 'BAJA';
  descripcion: string;
  referencia: string;
  fecha: string;
}
