export type Rol = 'ROLE_ADMIN' | 'ROLE_MEDICO' | 'ROLE_PACIENTE';

export const ROLES: readonly Rol[] = ['ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE'];

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
  usuarioId: number;
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
  uid?: number;
  nombre?: string;
  roles?: string[];
}

export interface SessionUser {
  id: number;
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
