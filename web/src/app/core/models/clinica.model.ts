export interface RecetaItem {
  medicamento: string;
  dosis?: string | null;
  frecuencia?: string | null;
  duracion?: string | null;
}

export interface AtencionRequest {
  diagnostico: string;
  tratamiento?: string | null;
  indicaciones?: string | null;
  observaciones?: string | null;
  pesoKg?: number | null;
  tallaCm?: number | null;
  presionArterial?: string | null;
  temperaturaC?: number | null;
  frecuenciaCardiaca?: number | null;
  receta: RecetaItem[];
}

export interface AtencionResponse {
  id: string;
  citaId: string;
  fecha: string;
  medicoNombre: string;
  especialidad: string;
  pacienteId: string;
  pacienteNombre: string;
  diagnostico: string;
  tratamiento?: string | null;
  indicaciones?: string | null;
  observaciones?: string | null;
  pesoKg?: number | null;
  tallaCm?: number | null;
  presionArterial?: string | null;
  temperaturaC?: number | null;
  frecuenciaCardiaca?: number | null;
  receta: RecetaItem[];
}

export interface PacienteClinicoResponse {
  pacienteId: string;
  nombreCompleto: string;
  dni: string;
  historialClinicoNumero: string;
  fechaNacimiento?: string | null;
  edad?: number | null;
  grupoSanguineo?: string | null;
  alergias?: string | null;
  antecedentes?: string | null;
}

export interface HistoriaClinicaResponse {
  paciente: PacienteClinicoResponse;
  atenciones: AtencionResponse[];
}

export interface DatosMedicosRequest {
  grupoSanguineo?: string | null;
  alergias?: string | null;
  antecedentes?: string | null;
}

export interface PacienteResumenResponse {
  pacienteId: string;
  nombreCompleto: string;
  dni: string;
  historialClinicoNumero: string;
  ultimaCita?: string | null;
  totalCitas: number;
  compartidoPor?: string | null;
}

export interface CompartidoResponse {
  medicoId: string;
  medicoNombre: string;
  especialidades: string[];
  nota?: string | null;
  fecha: string;
}

export interface CompartirPacienteRequest {
  medicoId: string;
  nota?: string | null;
}

export interface ValoracionRequest {
  puntuacion: number;
  comentario?: string | null;
}

export interface ValoracionResponse {
  id: string;
  citaId: string;
  medicoId: string;
  pacienteNombre: string;
  puntuacion: number;
  comentario?: string | null;
  createdAt: string;
}

export interface ValoracionesMedicoResponse {
  promedio: number;
  total: number;
  valoraciones: ValoracionResponse[];
}

export interface ReprogramarRequest {
  fechaHora: string;
}
