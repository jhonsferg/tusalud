export type ProgramaCronico = 'HIPERTENSION' | 'DIABETES' | 'ASMA' | 'OTRO';
export type EstadoPlan = 'ACTIVO' | 'COMPLETADO' | 'SUSPENDIDO';
export type ParametroSeguimiento =
  | 'PRESION_SISTOLICA'
  | 'PRESION_DIASTOLICA'
  | 'GLUCOSA_AYUNO'
  | 'HEMOGLOBINA_GLICOSILADA'
  | 'PESO_KG'
  | 'FLUJO_ESPIRATORIO';

export interface MetaPlan {
  parametro: ParametroSeguimiento;
  valorMin: number | null;
  valorMax: number | null;
  unidad: string;
}

export interface ControlPlan {
  id: string;
  numero: number;
  fechaProgramada: string;
  estado: 'PENDIENTE' | 'CUMPLIDO' | 'OMITIDO';
  vencido: boolean;
  citaId: string | null;
}

export interface MedicionPlan {
  id: string;
  numero: number;
  parametro: ParametroSeguimiento;
  valor: number;
  medidoEn: string;
  origen: 'PACIENTE' | 'PERSONAL';
  fueraDeRango: boolean;
}

export interface PlanResumen {
  id: string;
  paciente: string;
  programa: ProgramaCronico;
  estado: EstadoPlan;
  inicio: string;
  adherencia: number;
  controlesVencidos: number;
  medicionesFueraDeRango: number;
  riesgo: 'ALTO' | 'MEDIO' | 'BAJO';
}

export interface PlanDetalle {
  id: string;
  pacienteId: string;
  paciente: string;
  medico: string;
  programa: ProgramaCronico;
  diagnosticoCodigo: string | null;
  objetivos: string;
  intervenciones: string | null;
  medicacion: string | null;
  recetaId: string | null;
  estado: EstadoPlan;
  inicio: string;
  fin: string | null;
  frecuenciaControlDias: number;
  adherencia: number;
  riesgo: 'ALTO' | 'MEDIO' | 'BAJO';
  metas: MetaPlan[];
  controles: ControlPlan[];
  mediciones: MedicionPlan[];
}

export interface PlanRequest {
  pacienteId: string;
  programa: ProgramaCronico;
  diagnosticoCodigo?: string | null;
  objetivos: string;
  intervenciones?: string | null;
  medicacion?: string | null;
  recetaId?: string | null;
  inicio?: string | null;
  frecuenciaControlDias: number;
  cantidadControles: number;
  metas: MetaPlan[];
}

export interface MedicionPlanRequest {
  parametro: ParametroSeguimiento;
  valor: number;
  medidoEn?: string | null;
}
