export type TipoArco = 'ACCESO' | 'RECTIFICACION' | 'CANCELACION' | 'OPOSICION';

export type EstadoArco = 'RECIBIDA' | 'EN_REVISION' | 'ATENDIDA' | 'PARCIAL' | 'RECHAZADA';

export type CampoRectificable =
  'NOMBRE' | 'APELLIDO' | 'TELEFONO' | 'EMAIL' | 'FECHA_NACIMIENTO' | 'DNI';

export type DecisionArco = 'ATENDER' | 'ATENDER_PARCIALMENTE' | 'RECHAZAR';

export type EstadoConsentimientoTratamiento =
  'OTORGADO' | 'REVOCADO' | 'SIN_REGISTRO' | 'NO_REQUIERE';

export type TipoIncidente =
  'ACCESO_NO_AUTORIZADO' | 'PERDIDA' | 'DIVULGACION' | 'ALTERACION' | 'RANSOMWARE' | 'OTRO';

export type SeveridadIncidente = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';

export type EstadoIncidente = 'ABIERTO' | 'EN_CONTENCION' | 'NOTIFICADO' | 'CERRADO';

export type AccionRetencion = 'ANONIMIZAR' | 'DEPURAR' | 'MARCAR';

export const ETIQUETA_TIPO_ARCO: Readonly<Record<TipoArco, string>> = {
  ACCESO: 'Acceso',
  RECTIFICACION: 'Rectificación',
  CANCELACION: 'Cancelación',
  OPOSICION: 'Oposición',
};

export const DESCRIPCION_TIPO_ARCO: Readonly<Record<TipoArco, string>> = {
  ACCESO: 'Obtener una copia de los datos personales que tenemos sobre usted.',
  RECTIFICACION: 'Corregir un dato inexacto o desactualizado.',
  CANCELACION: 'Pedir que se supriman los datos que ya no sean necesarios.',
  OPOSICION: 'Oponerse a un tratamiento que no es indispensable para su atención.',
};

export const ETIQUETA_ESTADO_ARCO: Readonly<Record<EstadoArco, string>> = {
  RECIBIDA: 'Recibida',
  EN_REVISION: 'En revisión',
  ATENDIDA: 'Atendida',
  PARCIAL: 'Atendida en parte',
  RECHAZADA: 'Rechazada',
};

export const SEVERIDAD_ESTADO_ARCO: Readonly<
  Record<EstadoArco, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
> = {
  RECIBIDA: 'info',
  EN_REVISION: 'warn',
  ATENDIDA: 'success',
  PARCIAL: 'warn',
  RECHAZADA: 'danger',
};

export const ETIQUETA_CAMPO_RECTIFICABLE: Readonly<Record<CampoRectificable, string>> = {
  NOMBRE: 'Nombre',
  APELLIDO: 'Apellido',
  TELEFONO: 'Teléfono',
  EMAIL: 'Correo electrónico',
  FECHA_NACIMIENTO: 'Fecha de nacimiento',
  DNI: 'Documento de identidad',
};

export const ETIQUETA_DECISION_ARCO: Readonly<Record<DecisionArco, string>> = {
  ATENDER: 'Atender',
  ATENDER_PARCIALMENTE: 'Atender en parte',
  RECHAZAR: 'Rechazar',
};

export const ETIQUETA_TIPO_INCIDENTE: Readonly<Record<TipoIncidente, string>> = {
  ACCESO_NO_AUTORIZADO: 'Acceso no autorizado',
  PERDIDA: 'Pérdida de datos o equipos',
  DIVULGACION: 'Divulgación indebida',
  ALTERACION: 'Alteración de datos',
  RANSOMWARE: 'Secuestro de información',
  OTRO: 'Otro',
};

export const ETIQUETA_SEVERIDAD_INCIDENTE: Readonly<Record<SeveridadIncidente, string>> = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
};

export const ETIQUETA_ESTADO_INCIDENTE: Readonly<Record<EstadoIncidente, string>> = {
  ABIERTO: 'Abierto',
  EN_CONTENCION: 'En contención',
  NOTIFICADO: 'Notificado',
  CERRADO: 'Cerrado',
};

export const ETIQUETA_ACCION_RETENCION: Readonly<Record<AccionRetencion, string>> = {
  ANONIMIZAR: 'Anonimizar',
  DEPURAR: 'Depurar',
  MARCAR: 'Marcar para revisión',
};

export interface PoliticaPublica {
  version: number;
  titulo: string;
  contenido: string;
  vigenteDesde: string;
  hash: string;
}

export interface PoliticaPrivacidad {
  id: string;
  version: number;
  titulo: string;
  contenido: string;
  vigenteDesde: string;
  publicada: boolean;
  hash: string;
  creadaEn: string;
  aceptaciones: number;
}

export interface PoliticaRequest {
  titulo: string;
  contenido: string;
  vigenteDesde: string;
}

export interface EstadoAceptacion {
  versionVigente: number | null;
  politicaId: string | null;
  aceptada: boolean;
  aceptadaEn: string | null;
  versionAceptada: number | null;
}

export interface BancoDatos {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string;
  finalidadPrincipal: string;
  categoriasDatos: string;
  contieneSensibles: boolean;
  responsable: string;
  ubicacion: string;
  medidasSeguridad: string;
  transferenciaTransfronteriza: boolean;
  numeroInscripcion: string | null;
  fechaInscripcion: string | null;
  activo: boolean;
  actualizadoEn: string;
}

export type BancoDatosRequest = Omit<BancoDatos, 'id' | 'actualizadoEn'>;

export interface FinalidadTratamiento {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string;
  bancoId: string | null;
  bancoNombre: string | null;
  baseLegal: string;
  obligatoria: boolean;
  categoriaDatos: string;
  plazoRetencionDias: number | null;
  activa: boolean;
}

export type FinalidadRequest = Omit<FinalidadTratamiento, 'id' | 'bancoNombre'>;

export interface EncargadoTratamiento {
  id: string;
  razonSocial: string;
  ruc: string | null;
  servicio: string;
  categoriasDatos: string;
  pais: string;
  transfronterizo: boolean;
  contratoVigenteHasta: string | null;
  contratoVencido: boolean;
  contacto: string | null;
  activo: boolean;
}

export type EncargadoRequest = Omit<EncargadoTratamiento, 'id' | 'contratoVencido'>;

export interface RegistroTratamientos {
  bancos: BancoDatos[];
  finalidades: FinalidadTratamiento[];
  encargados: EncargadoTratamiento[];
}

export interface ConsentimientoTratamientoEstado {
  finalidadId: string;
  codigo: string;
  nombre: string;
  descripcion: string;
  baseLegal: string;
  obligatoria: boolean;
  estado: EstadoConsentimientoTratamiento;
  desde: string | null;
}

export interface HistorialConsentimiento {
  id: string;
  finalidad: string;
  accion: 'OTORGAR' | 'REVOCAR';
  origen: string;
  politicaVersion: number | null;
  motivo: string | null;
  registradoEn: string;
}

export interface BloqueoCancelacion {
  categoria: string;
  estado: 'BLOQUEADO' | 'CANCELABLE';
  motivo: string;
  baseLegal: string;
}

export interface PreviaCancelacion {
  categorias: BloqueoCancelacion[];
  hayBloqueos: boolean;
}

export interface SolicitudArco {
  id: string;
  codigo: string;
  tipo: TipoArco;
  estado: EstadoArco;
  descripcion: string;
  campo: CampoRectificable | null;
  valorPropuesto: string | null;
  solicitante: string;
  usuarioId: string;
  responsable: string | null;
  recibidaEn: string;
  venceEn: string;
  vencida: boolean;
  resueltaEn: string | null;
  resolucion: string | null;
  tieneExportacion: boolean;
}

export interface EventoArco {
  id: string;
  accion: string;
  detalle: string | null;
  actor: string;
  registradoEn: string;
}

export interface SolicitudArcoDetalle {
  solicitud: SolicitudArco;
  finalidadId: string | null;
  finalidad: string | null;
  resultado: BloqueoCancelacion[];
  eventos: EventoArco[];
}

export interface SolicitudArcoRequest {
  tipo: TipoArco;
  descripcion: string;
  campo: CampoRectificable | null;
  valorPropuesto: string | null;
  finalidadId: string | null;
}

export interface ResolucionArcoRequest {
  decision: DecisionArco;
  resolucion: string;
}

export interface AccesoDatos {
  id: string;
  fecha: string;
  accion: string;
  detalle: string | null;
  realizadoPor: string;
}

export interface PoliticaRetencion {
  id: string;
  tipoDato: string;
  nombre: string;
  descripcion: string;
  plazoDias: number;
  accion: AccionRetencion;
  baseLegal: string;
  activa: boolean;
  ultimaEjecucion: string | null;
}

export interface EjecucionRetencion {
  id: string;
  politica: string;
  ejecutadaEn: string;
  afectados: number;
  detalle: string | null;
}

export interface Incidente {
  id: string;
  codigo: string;
  tipo: TipoIncidente;
  severidad: SeveridadIncidente;
  estado: EstadoIncidente;
  descripcion: string;
  datosAfectados: string;
  titularesAfectados: number;
  detectadoEn: string;
  notificarAutoridadAntesDe: string;
  autoridadNotificadaEn: string | null;
  titularesNotificadosEn: string | null;
  contencion: string | null;
  causaRaiz: string | null;
  cerradoEn: string | null;
  responsable: string | null;
  registradoPor: string;
  vencido: boolean;
  horasRestantes: number;
}

export interface IncidenteRequest {
  tipo: TipoIncidente;
  severidad: SeveridadIncidente;
  descripcion: string;
  datosAfectados: string;
  titularesAfectados: number;
  detectadoEn: string | null;
  contencion: string | null;
}

export interface IncidenteActualizacionRequest {
  estado: EstadoIncidente;
  contencion: string | null;
  causaRaiz: string | null;
  autoridadNotificada: boolean;
  titularesNotificados: boolean;
  responsableId: string | null;
}

export interface EstadoPrivacidad {
  solicitudesAbiertas: number;
  solicitudesVencidas: number;
  incidentesAbiertos: number;
  incidentesVencidos: number;
  versionPolitica: number | null;
  ultimaRetencion: string | null;
}
