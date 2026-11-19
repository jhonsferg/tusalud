export type TipoDocumentoMedico =
  'CERTIFICADO' | 'DESCANSO' | 'INFORME' | 'REFERENCIA' | 'CONTRARREFERENCIA' | 'EPICRISIS';

export type EstadoDocumentoMedico = 'BORRADOR' | 'EMITIDO' | 'ANULADO';
export type EstadoReferencia = 'ENVIADA' | 'RECIBIDA' | 'ATENDIDA' | 'RECHAZADA';
export type PrioridadReferencia = 'NORMAL' | 'PREFERENTE' | 'URGENTE';
export type NivelEstablecimiento =
  'PRIMER_NIVEL' | 'SEGUNDO_NIVEL' | 'TERCER_NIVEL' | 'ESPECIALIZADO';
export type EstadoConsentimiento = 'PENDIENTE_FIRMA' | 'FIRMADO' | 'REVOCADO' | 'VENCIDO';
export type FirmanteTipo = 'PACIENTE' | 'REPRESENTANTE';
export type ViaFirma = 'APP_PACIENTE' | 'CONSULTORIO';

export const TIPOS_DOCUMENTO_MEDICO: readonly TipoDocumentoMedico[] = [
  'CERTIFICADO',
  'DESCANSO',
  'INFORME',
  'REFERENCIA',
  'CONTRARREFERENCIA',
  'EPICRISIS',
];

export const ETIQUETA_TIPO_DOCUMENTO_MEDICO: Readonly<Record<TipoDocumentoMedico, string>> = {
  CERTIFICADO: 'Certificado médico',
  DESCANSO: 'Descanso médico',
  INFORME: 'Informe médico',
  REFERENCIA: 'Referencia',
  CONTRARREFERENCIA: 'Contrarreferencia',
  EPICRISIS: 'Epicrisis',
};

export const ETIQUETA_ESTADO_DOCUMENTO_MEDICO: Readonly<Record<EstadoDocumentoMedico, string>> = {
  BORRADOR: 'Borrador',
  EMITIDO: 'Emitido',
  ANULADO: 'Anulado',
};

export const ETIQUETA_ESTADO_REFERENCIA: Readonly<Record<EstadoReferencia, string>> = {
  ENVIADA: 'Enviada',
  RECIBIDA: 'Recibida',
  ATENDIDA: 'Atendida',
  RECHAZADA: 'Rechazada',
};

export const ETIQUETA_NIVEL_ESTABLECIMIENTO: Readonly<Record<NivelEstablecimiento, string>> = {
  PRIMER_NIVEL: 'Primer nivel',
  SEGUNDO_NIVEL: 'Segundo nivel',
  TERCER_NIVEL: 'Tercer nivel',
  ESPECIALIZADO: 'Especializado',
};

export const ETIQUETA_ESTADO_CONSENTIMIENTO: Readonly<Record<EstadoConsentimiento, string>> = {
  PENDIENTE_FIRMA: 'Pendiente de firma',
  FIRMADO: 'Firmado',
  REVOCADO: 'Revocado',
  VENCIDO: 'Vencido',
};

export interface ReferenciaDocumento {
  id: string;
  destinoId: string;
  destinoNombre: string;
  especialidad: string;
  prioridad: PrioridadReferencia;
  estado: EstadoReferencia;
  motivoRechazo?: string | null;
}

export interface DocumentoMedicoResumen {
  id: string;
  tipo: TipoDocumentoMedico;
  tipoEtiqueta: string;
  estado: EstadoDocumentoMedico;
  numero?: string | null;
  pacienteId: string;
  pacienteNombre: string;
  medicoNombre: string;
  medicoColegiatura: string;
  creadoEn: string;
  emitidoEn?: string | null;
  anuladoEn?: string | null;
  motivoAnulacion?: string | null;
  ingresoId?: string | null;
  atencionId?: string | null;
  referencia?: ReferenciaDocumento | null;
}

export interface DocumentoMedicoDetalle {
  resumen: DocumentoMedicoResumen;
  contenido: Record<string, unknown>;
  autorizaDiagnostico: boolean;
  codigoVerificacion?: string | null;
  documentoOrigenId?: string | null;
  referenciaId?: string | null;
  reemplazado: boolean;
  contrarreferencias: string[];
}

export interface DocumentoMedicoRequest {
  pacienteId: string;
  tipo: TipoDocumentoMedico;
  atencionId?: string | null;
  ingresoId?: string | null;
  documentoOrigenId?: string | null;
  referenciaId?: string | null;
  autorizaDiagnostico: boolean;
  contenido: Record<string, unknown>;
}

export interface ActualizarDocumentoRequest {
  autorizaDiagnostico: boolean;
  contenido: Record<string, unknown>;
}

export interface ReferenciaEstadoRequest {
  estado: EstadoReferencia;
  motivoRechazo?: string | null;
}

export interface DocumentoMinimo {
  tipo: TipoDocumentoMedico;
  tipoEtiqueta: string;
  estado: EstadoDocumentoMedico;
  numero?: string | null;
  fecha: string;
}

export interface EstablecimientoSalud {
  id: string;
  codigoIpress?: string | null;
  nombre: string;
  nivel: NivelEstablecimiento;
  distrito?: string | null;
  contacto?: string | null;
  activo: boolean;
}

export interface EstablecimientoRequest {
  codigoIpress?: string | null;
  nombre: string;
  nivel: NivelEstablecimiento;
  distrito?: string | null;
  contacto?: string | null;
  activo: boolean;
}

export interface ConsentimientoPlantilla {
  id: string;
  codigo: string;
  nombre: string;
  texto: string;
  riesgos: string;
  beneficios: string;
  alternativas: string;
  requiereTestigo: boolean;
  vigenciaDias: number;
  version: number;
  activa: boolean;
}

export type ConsentimientoPlantillaRequest = Omit<ConsentimientoPlantilla, 'id' | 'version'>;

export interface ConsentimientoResumen {
  id: string;
  plantillaNombre: string;
  procedimiento: string;
  estado: EstadoConsentimiento;
  pacienteId: string;
  pacienteNombre: string;
  medicoNombre: string;
  requiereTestigo: boolean;
  creadoEn: string;
  firmadoEn?: string | null;
  venceEn?: string | null;
  firmadoVia?: ViaFirma | null;
  ingresoId?: string | null;
  atencionId?: string | null;
}

export interface ConsentimientoDetalle {
  resumen: ConsentimientoResumen;
  plantillaVersion: number;
  textoAceptado: string;
  firmanteTipo?: FirmanteTipo | null;
  firmanteNombre?: string | null;
  firmanteDni?: string | null;
  firmanteParentesco?: string | null;
  testigoNombre?: string | null;
  testigoDni?: string | null;
  motivoRevocacion?: string | null;
  codigoVerificacion?: string | null;
  revocadoEn?: string | null;
}

export interface ConsentimientoSolicitudRequest {
  plantillaId: string;
  pacienteId: string;
  atencionId?: string | null;
  ingresoId?: string | null;
  ordenImagenId?: string | null;
  procedimiento: string;
}

export interface FirmaConsentimientoRequest {
  firmaPng: string;
  firmanteTipo: FirmanteTipo;
  nombre: string;
  dni: string;
  parentesco?: string | null;
  testigoNombre?: string | null;
  testigoDni?: string | null;
  via: ViaFirma;
}

export interface ResumenIngresoDocumentos {
  ingresoId: string;
  documentos: DocumentoMinimo[];
  consentimientos: ConsentimientoResumen[];
}

export type ResultadoVerificacion = 'VALIDO' | 'ANULADO' | 'VENCIDO' | 'NO_ENCONTRADO';

export interface VerificacionPublica {
  resultado: ResultadoVerificacion;
  tipo?: string | null;
  emitidoEn?: string | null;
  medico?: string | null;
  colegiatura?: string | null;
  establecimiento?: string | null;
}
