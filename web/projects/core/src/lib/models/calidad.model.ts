export type TipoReporteSeguridad = 'EVENTO_ADVERSO' | 'CASI_INCIDENTE' | 'INCIDENTE_SIN_DANO';
export type SeveridadEvento = 'LEVE' | 'MODERADO' | 'GRAVE' | 'CENTINELA';
export type EstadoEvento = 'NOTIFICADO' | 'EN_ANALISIS' | 'EN_ACCION' | 'CERRADO';
export type MetodoAnalisis = 'CINCO_PORQUES' | 'ESPINA_PESCADO';

export interface EventoRequest {
  tipo: TipoReporteSeguridad;
  severidad: SeveridadEvento;
  area: string;
  descripcion: string;
  anonimo: boolean;
  ocurridoEn: string;
  pacienteId?: string | null;
  cirugiaId?: string | null;
  dispensacionId?: string | null;
}

export interface EventoResumen {
  id: string;
  codigo: string;
  tipo: TipoReporteSeguridad;
  severidad: SeveridadEvento;
  area: string;
  estado: EstadoEvento;
  anonimo: boolean;
  notificante: string | null;
  ocurridoEn: string;
  notificadoEn: string;
  accionesPendientes: number;
}

export interface CausaEvento {
  posicion: number;
  categoria: string;
  texto: string;
}

export interface AccionEvento {
  id: string;
  descripcion: string;
  responsableId: string;
  responsable: string;
  fechaLimite: string;
  estado: 'PENDIENTE' | 'HECHA';
  vencida: boolean;
  completadaEn: string | null;
}

export interface EventoDetalle {
  id: string;
  codigo: string;
  tipo: TipoReporteSeguridad;
  severidad: SeveridadEvento;
  area: string;
  estado: EstadoEvento;
  anonimo: boolean;
  notificante: string | null;
  descripcion: string;
  paciente: string | null;
  cirugiaId: string | null;
  dispensacionId: string | null;
  clasificacion: string | null;
  analisisMetodo: MetodoAnalisis | null;
  analisisConclusion: string | null;
  causas: CausaEvento[];
  acciones: AccionEvento[];
  ocurridoEn: string;
  notificadoEn: string;
  cerradoEn: string | null;
  cerradoPor: string | null;
  verificacion: string | null;
  accionEficaz: boolean | null;
}

export interface AnalisisEventoRequest {
  metodo: MetodoAnalisis;
  causas: { categoria: string; texto: string }[];
  conclusion: string;
  clasificacion: string | null;
}

export interface AccionEventoRequest {
  descripcion: string;
  responsableId: string;
  fechaLimite: string;
}

export interface CierreEventoRequest {
  verificacion: string;
  accionEficaz: boolean;
}

export interface TableroSeguridad {
  total: number;
  porEstado: Record<string, number>;
  porSeveridad: Record<string, number>;
  porArea: Record<string, number>;
  porMes: Record<string, number>;
  accionesVencidas: number;
  diasMedioCierre: number;
  casiIncidentes: number;
  eventosAdversos: number;
}

export interface GuiaResumen {
  id: string;
  codigo: string;
  version: number;
  titulo: string;
  especialidad: string | null;
  resumen: string;
  estado: 'VIGENTE' | 'OBSOLETA' | 'BORRADOR';
  vigenteDesde: string;
  vigenteHasta: string | null;
  porValidar: boolean;
  codigosCie10: string[];
}

export interface GuiaDetalle {
  guia: GuiaResumen;
  contenido: string;
  versiones: GuiaResumen[];
}

export interface GuiaRequest {
  codigo: string;
  titulo: string;
  especialidadId?: string | null;
  resumen: string;
  contenido: string;
  codigosCie10: string[];
  vigenteDesde: string;
  porValidar: boolean;
}
