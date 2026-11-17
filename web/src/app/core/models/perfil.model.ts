export interface PerfilResponse {
  usuarioId: number;
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
  medicoId?: number | null;
  colegiatura?: string | null;
  consultorio?: string | null;
  especialidad?: string | null;
  especialidades?: string[];
  avatar?: string | null;
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
