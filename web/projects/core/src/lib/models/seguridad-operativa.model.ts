export type SeveridadSeguridad = 'INFO' | 'AVISO' | 'ALTA';

export type TipoEventoSeguridad =
  | 'LOGIN_FALLIDO'
  | 'LOGIN_CUENTA_INACTIVA'
  | 'LOGIN_FALLIDOS_REPETIDOS'
  | 'DISPOSITIVO_NUEVO'
  | 'BLOQUEO_INTENTOS'
  | 'MFA_ACTIVADO'
  | 'MFA_DESACTIVADO'
  | 'CONTRASENA_CAMBIADA'
  | 'ROLES_CAMBIADOS'
  | 'CUENTA_ACTIVADA'
  | 'CUENTA_DESACTIVADA'
  | 'ACCESO_DENEGADO'
  | 'ACCESO_DENEGADO_REPETIDO'
  | 'DESCARGA_MASIVA';

export const TIPOS_EVENTO_SEGURIDAD: readonly { value: TipoEventoSeguridad; label: string }[] = [
  { value: 'LOGIN_FALLIDO', label: 'Inicio de sesión fallido' },
  { value: 'LOGIN_CUENTA_INACTIVA', label: 'Intento con cuenta inactiva' },
  { value: 'LOGIN_FALLIDOS_REPETIDOS', label: 'Inicios fallidos repetidos' },
  { value: 'DISPOSITIVO_NUEVO', label: 'Dispositivo nuevo' },
  { value: 'BLOQUEO_INTENTOS', label: 'Bloqueo por intentos' },
  { value: 'MFA_ACTIVADO', label: 'Verificación en dos pasos activada' },
  { value: 'MFA_DESACTIVADO', label: 'Verificación en dos pasos desactivada' },
  { value: 'CONTRASENA_CAMBIADA', label: 'Contraseña cambiada' },
  { value: 'ROLES_CAMBIADOS', label: 'Roles modificados' },
  { value: 'CUENTA_ACTIVADA', label: 'Cuenta activada' },
  { value: 'CUENTA_DESACTIVADA', label: 'Cuenta desactivada' },
  { value: 'ACCESO_DENEGADO', label: 'Acceso denegado' },
  { value: 'ACCESO_DENEGADO_REPETIDO', label: 'Accesos denegados repetidos' },
  { value: 'DESCARGA_MASIVA', label: 'Descarga masiva de archivos' },
];

export const SEVERIDADES_SEGURIDAD: readonly { value: SeveridadSeguridad; label: string }[] = [
  { value: 'ALTA', label: 'Alta' },
  { value: 'AVISO', label: 'Aviso' },
  { value: 'INFO', label: 'Informativa' },
];

export interface EventoSeguridadResponse {
  id: string;
  tipo: TipoEventoSeguridad;
  etiqueta: string;
  severidad: SeveridadSeguridad;
  usuarioId?: string | null;
  usuarioNombre?: string | null;
  actorNombre?: string | null;
  ip?: string | null;
  dispositivo?: string | null;
  detalle?: string | null;
  creadoEn: string;
}

export interface PaginaEventosSeguridadResponse {
  contenido: EventoSeguridadResponse[];
  pagina: number;
  tamano: number;
  total: number;
  totalPaginas: number;
}

export interface FiltroEventosSeguridad {
  tipo?: TipoEventoSeguridad | null;
  severidad?: SeveridadSeguridad | null;
  desde?: string | null;
  hasta?: string | null;
  usuarioId?: string | null;
  pagina?: number;
  tamano?: number;
}

export interface ConteoTipoSeguridad {
  tipo: TipoEventoSeguridad;
  etiqueta: string;
  severidad: SeveridadSeguridad;
  cantidad: number;
}

export interface VentanaSeguridad {
  total: number;
  altas: number;
  avisos: number;
  porTipo: ConteoTipoSeguridad[];
}

export interface ResumenSeguridadResponse {
  ultimas24Horas: VentanaSeguridad;
  ultimos7Dias: VentanaSeguridad;
  iniciosInusuales: EventoSeguridadResponse[];
}

export interface ComponenteEstado {
  nombre: string;
  estado: 'OK' | 'CAIDO';
  latenciaMs?: number | null;
  detalle?: string | null;
}

export interface EstadoSistemaResponse {
  estadoGeneral: 'OK' | 'DEGRADADO';
  version: string;
  iniciadoEn: string;
  segundosEnMarcha: number;
  componentes: ComponenteEstado[];
  migraciones: { disponible: boolean; aplicadas: number; ultima?: string | null };
}

export interface UsoProposito {
  proposito: string;
  objetos: number;
  bytes: number;
}

export interface UsoBucket {
  bucket: string;
  objetos: number;
  bytes: number;
  propositos: UsoProposito[];
}

export interface UsoAlmacenamientoResponse {
  totalObjetos: number;
  totalBytes: number;
  buckets: UsoBucket[];
}

export interface EvolucionPendienteResponse {
  ingresoId: string;
  codigo: string;
  cama: string;
  area: string;
  paciente: string;
  ingresadoEn: string;
  ultimaEvolucion?: string | null;
  horasSinRegistro: number;
}

export interface MotivoRequest {
  motivo: string;
}
