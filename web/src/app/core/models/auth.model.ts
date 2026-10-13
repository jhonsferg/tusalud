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
  user: SessionUser;
}
