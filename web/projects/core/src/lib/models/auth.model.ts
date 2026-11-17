export type Rol =
  | 'ROLE_ADMIN'
  | 'ROLE_MEDICO'
  | 'ROLE_PACIENTE'
  | 'ROLE_LABORATORIO'
  | 'ROLE_FARMACIA'
  | 'ROLE_ALMACEN'
  | 'ROLE_RECEPCION'
  | 'ROLE_CAJA'
  | 'ROLE_ENFERMERIA';

export const ROLES: readonly Rol[] = [
  'ROLE_ADMIN',
  'ROLE_MEDICO',
  'ROLE_PACIENTE',
  'ROLE_LABORATORIO',
  'ROLE_FARMACIA',
  'ROLE_ALMACEN',
  'ROLE_RECEPCION',
  'ROLE_CAJA',
  'ROLE_ENFERMERIA',
];

export const ROLES_PERSONAL: readonly Rol[] = [
  'ROLE_ADMIN',
  'ROLE_LABORATORIO',
  'ROLE_FARMACIA',
  'ROLE_ALMACEN',
  'ROLE_RECEPCION',
  'ROLE_CAJA',
  'ROLE_ENFERMERIA',
];

export const ETIQUETA_ROL: Readonly<Record<string, string>> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_MEDICO: 'Médico',
  ROLE_PACIENTE: 'Paciente',
  ROLE_LABORATORIO: 'Laboratorio',
  ROLE_FARMACIA: 'Farmacia',
  ROLE_ALMACEN: 'Almacén',
  ROLE_RECEPCION: 'Recepción',
  ROLE_CAJA: 'Caja',
  ROLE_ENFERMERIA: 'Enfermería',
};

export const DESCRIPCION_ROL: Readonly<Record<string, string>> = {
  ROLE_ADMIN: 'Gestiona usuarios, roles, tarifas, cupones y pagos.',
  ROLE_MEDICO: 'Atiende pacientes y administra su agenda. Requiere perfil médico.',
  ROLE_PACIENTE: 'Reserva citas y paga consultas. Requiere perfil de paciente.',
  ROLE_LABORATORIO: 'Procesa órdenes de laboratorio y registra resultados.',
  ROLE_FARMACIA: 'Dispensa recetas y vende productos de farmacia.',
  ROLE_ALMACEN: 'Controla el inventario, los lotes y los proveedores.',
  ROLE_RECEPCION:
    'Registra la llegada de pacientes, administra la cola y reserva citas en mostrador.',
  ROLE_CAJA: 'Abre y cierra la caja, cobra y emite comprobantes.',
  ROLE_ENFERMERIA: 'Atiende el triaje, las camas y las evoluciones de enfermería.',
};

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  telefono?: string | null;
  dni: string;
  fechaNacimiento: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  usuarioId: string;
  email: string;
  nombreCompleto: string;
  roles: string[];
  refreshToken?: string | null;
  refreshExpiresIn?: number | null;
}

export interface JwtPayload {
  sub?: string;
  iss?: string;
  iat?: number;
  exp?: number;
  uid?: string;
  nombre?: string;
  roles?: string[];
}

export interface SessionUser {
  id: string;
  email: string;
  nombreCompleto: string;
  roles: Rol[];
}

export interface Session {
  token: string;
  expiresAt: number;
  refreshToken?: string;
  refreshExpiresAt?: number;
  user: SessionUser;
}

export interface LoginResponse extends Omit<AuthResponse, 'token'> {
  token?: string | null;
  mfaRequerido?: boolean;
  mfaToken?: string | null;
}

export interface MfaChallenge {
  mfaToken: string;
}

export type LoginResult = { sesion: SessionUser } | { mfa: MfaChallenge };

export interface RefreshRequest {
  refreshToken: string;
}

export interface MfaVerifyRequest {
  mfaToken: string;
  codigo: string;
}

export interface MfaEstadoResponse {
  activo: boolean;
  codigosRecuperacionRestantes: number;
}

export interface MfaConfiguracionResponse {
  secreto: string;
  otpauthUri: string;
}

export interface MfaActivacionResponse {
  codigosRecuperacion: string[];
}

export interface MfaDesactivarRequest {
  password: string;
  codigo: string;
}
