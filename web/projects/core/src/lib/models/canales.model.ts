import { TipoNotificacion } from './notificacion.model';

export type CanalExterno = 'CORREO' | 'SMS' | 'WHATSAPP';

export type EstadoContacto = 'SIN_CONFIGURAR' | 'PENDIENTE_VERIFICACION' | 'ACTIVO' | 'BAJA';

export type EstadoEntrega = 'PENDIENTE' | 'ENVIADO' | 'ENTREGADO' | 'FALLIDO' | 'REBOTADO';

export const CANALES_EXTERNOS: readonly CanalExterno[] = ['CORREO', 'SMS', 'WHATSAPP'];

export const ETIQUETA_CANAL: Readonly<Record<CanalExterno, string>> = {
  CORREO: 'Correo electrónico',
  SMS: 'Mensaje de texto',
  WHATSAPP: 'WhatsApp',
};

export const ICONO_CANAL: Readonly<Record<CanalExterno, string>> = {
  CORREO: 'pi-envelope',
  SMS: 'pi-mobile',
  WHATSAPP: 'pi-whatsapp',
};

export const ETIQUETA_ESTADO_CONTACTO: Readonly<Record<EstadoContacto, string>> = {
  SIN_CONFIGURAR: 'Sin activar',
  PENDIENTE_VERIFICACION: 'Pendiente de verificar',
  ACTIVO: 'Activo',
  BAJA: 'Dado de baja',
};

export const ETIQUETA_ESTADO_ENTREGA: Readonly<Record<EstadoEntrega, string>> = {
  PENDIENTE: 'Pendiente',
  ENVIADO: 'Enviado',
  ENTREGADO: 'Entregado',
  FALLIDO: 'Fallido',
  REBOTADO: 'Rebotado',
};

export const ESTADOS_ENTREGA: readonly EstadoEntrega[] = [
  'PENDIENTE',
  'ENVIADO',
  'ENTREGADO',
  'FALLIDO',
  'REBOTADO',
];

export interface ContactoCanal {
  canal: CanalExterno;
  etiqueta: string;
  destinoMascara: string;
  estado: EstadoContacto;
  consentimientoEn: string | null;
  verificadoEn: string | null;
  destinoFijo: boolean;
}

export interface TipoPreferencia {
  tipo: TipoNotificacion;
  etiqueta: string;
  categoria: string;
  obligatoria: boolean;
  activos: Record<CanalExterno, boolean>;
}

export interface SilencioCanales {
  activo: boolean;
  desde: string;
  hasta: string;
}

export interface MisCanales {
  canales: ContactoCanal[];
  preferencias: TipoPreferencia[];
  silencio: SilencioCanales;
}

export interface SolicitudContactoRequest {
  destino?: string | null;
  consentimiento: boolean;
}

export interface PreferenciaCanalRequest {
  tipo: TipoNotificacion;
  canal: CanalExterno;
  activo: boolean;
}

export interface PreferenciasCanalesRequest {
  preferencias?: PreferenciaCanalRequest[];
  silencio?: SilencioCanales;
}

export interface PlantillaMensaje {
  tipo: TipoNotificacion;
  etiqueta: string;
  categoria: string;
  canal: CanalExterno;
  asunto: string | null;
  cuerpo: string;
  version: number;
  personalizada: boolean;
  actualizadaEn: string | null;
  actualizadaPor: string | null;
  motivo: string | null;
}

export interface PlantillasMensaje {
  variables: string[];
  plantillas: PlantillaMensaje[];
}

export interface PlantillaMensajeRequest {
  asunto: string | null;
  cuerpo: string;
  motivo: string;
}

export interface EntregaNotificacion {
  id: string;
  tipo: TipoNotificacion;
  canal: CanalExterno;
  destino: string;
  estado: EstadoEntrega;
  intentos: number;
  proximoIntento: string | null;
  motivo: string | null;
  plantillaVersion: number | null;
  urgente: boolean;
  creadoEn: string;
  enviadoEn: string | null;
  entregadoEn: string | null;
}

export interface PaginaEntregas {
  contenido: EntregaNotificacion[];
  pagina: number;
  tamano: number;
  total: number;
  totalPaginas: number;
}

export interface FiltroEntregas {
  canal?: CanalExterno | null;
  estado?: EstadoEntrega | null;
  desde?: string | null;
  hasta?: string | null;
  pagina?: number;
  tamano?: number;
}

export interface ResumenCanal {
  canal: CanalExterno;
  porEstado: Partial<Record<EstadoEntrega, number>>;
  total: number;
}

export interface ResumenEntregas {
  dias: number;
  total: number;
  canales: ResumenCanal[];
}

export interface MensajeSimulado {
  id: string;
  canal: CanalExterno;
  destino: string;
  asunto: string | null;
  texto: string;
  fecha: string;
}

export interface ProveedoresCanales {
  proveedores: Record<CanalExterno, string>;
  workerHabilitado: boolean;
  limitePorHora: number;
}

export type TipoEjecucionRespaldo = 'RESPALDO' | 'PRUEBA_RESTAURACION';
export type EstadoEjecucionRespaldo = 'EXITOSO' | 'FALLIDO';
export type SemaforoRespaldos = 'VERDE' | 'AMARILLO' | 'ROJO';

export const ETIQUETA_TIPO_RESPALDO: Readonly<Record<TipoEjecucionRespaldo, string>> = {
  RESPALDO: 'Respaldo',
  PRUEBA_RESTAURACION: 'Prueba de restauración',
};

export interface RespaldoEjecucion {
  id: string;
  tipo: TipoEjecucionRespaldo;
  estado: EstadoEjecucionRespaldo;
  alcance: 'BASE' | 'ALMACEN' | 'COMPLETO';
  categoria: 'DIARIO' | 'SEMANAL' | 'MENSUAL' | null;
  iniciadoEn: string;
  finalizadoEn: string;
  duracionSeg: number;
  bytesTotal: number | null;
  archivo: string | null;
  checksum: string | null;
  migracionesValidadas: number | null;
  tablasValidadas: number | null;
  objetosAlmacen: number | null;
  copiaRemota: boolean;
  detalle: string | null;
}

export interface EstadoRespaldos {
  ultimoRespaldoExitoso: RespaldoEjecucion | null;
  ultimoRespaldoFallido: RespaldoEjecucion | null;
  ultimaPruebaExitosa: RespaldoEjecucion | null;
  ultimaPrueba: RespaldoEjecucion | null;
  horasDesdeUltimoRespaldo: number | null;
  diasDesdeUltimaPrueba: number | null;
  respaldoVencido: boolean;
  pruebaVencida: boolean;
  maxHorasSinRespaldo: number;
  maxDiasSinPrueba: number;
  rpoObjetivoHoras: number;
  rtoObjetivoMinutos: number;
  semaforo: SemaforoRespaldos;
}
