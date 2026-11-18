export interface PerfilResponse {
  usuarioId: string;
  nombre: string;
  apellido: string;
  nombreCompleto: string;
  email: string;
  telefono?: string | null;
  roles: string[];
  createdAt: string;
  dni?: string | null;
  fechaNacimiento?: string | null;
  historialClinicoNumero?: string | null;
  medicoId?: string | null;
  colegiatura?: string | null;
  consultorio?: string | null;
  especialidad?: string | null;
  especialidades?: string[];
  avatarId?: string | null;
  mfaActivo?: boolean;
}

export interface PerfilUpdateRequest {
  nombre: string;
  apellido: string;
  telefono?: string | null;
  fechaNacimiento?: string | null;
  consultorio?: string | null;
}

export interface PasswordChangeRequest {
  passwordActual: string;
  passwordNueva: string;
}
