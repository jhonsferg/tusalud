export interface TopMedicoResponse {
  medicoId: number;
  nombre: string;
  citasAtendidas: number;
}

export interface IngresoMesResponse {
  mes: string;
  total: number;
}

export interface CitasEspecialidadResponse {
  especialidad: string;
  cantidad: number;
}

export interface EstadisticasResponse {
  totalPacientes: number;
  totalMedicos: number;
  totalEspecialidades: number;
  citasHoy: number;
  citasPendientes: number;
  ingresosMes: number;
  ingresosTotales: number;
  topMedicos: TopMedicoResponse[];
  ingresosPorMes: IngresoMesResponse[];
  citasPorEspecialidad: CitasEspecialidadResponse[];
}

export interface UsuarioAdminResponse {
  id: number;
  nombreCompleto: string;
  email: string;
  telefono?: string | null;
  roles: string[];
  estado: 'ACTIVO' | 'INACTIVO';
  createdAt: string;
  ultimoAcceso?: string | null;
}
