export type EstadoDosis = 'APLICADA' | 'ATRASADA' | 'PROXIMA' | 'PENDIENTE' | 'FUERA_DE_EDAD';
export type EstadoRegistroVacunacion = 'VIGENTE' | 'ANULADO';
export type IndicadorCrecimiento = 'PESO_EDAD' | 'TALLA_EDAD' | 'PC_EDAD' | 'IMC_EDAD';
export type NivelAlerta = 'NORMAL' | 'ATENCION' | 'ALERTA';

export interface Vacuna {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
}

export interface EsquemaDosis {
  vacunaId: string;
  vacunaCodigo: string;
  vacunaNombre: string;
  numeroDosis: number;
  etiqueta: string;
  edadMinimaDias: number;
  edadRecomendadaDias: number;
  edadMaximaDias: number | null;
  intervaloMinimoDias: number | null;
}

export interface EsquemaVacunacion {
  id: string;
  nombre: string;
  vigenteDesde: string;
  vigenteHasta: string | null;
  porValidar: boolean;
  activo: boolean;
  fuente: string | null;
  dosis: EsquemaDosis[];
}

export interface DosisCarne {
  vacunaId: string;
  vacunaCodigo: string;
  vacunaNombre: string;
  numeroDosis: number;
  etiqueta: string;
  estado: EstadoDosis;
  fechaRecomendada: string;
  fechaAplicada: string | null;
  atrasoDias: number | null;
  registroId: string | null;
}

export interface RegistroVacuna {
  id: string;
  vacunaId: string;
  vacunaNombre: string;
  numeroDosis: number;
  fechaAplicacion: string;
  lote: string;
  establecimiento: string;
  responsableNombre: string;
  estado: EstadoRegistroVacunacion;
  observaciones: string | null;
  motivoAnulacion: string | null;
}

export interface CarneVacunacion {
  pacienteId: string;
  pacienteNombre: string;
  dni: string;
  fechaNacimiento: string | null;
  edadMeses: number | null;
  sexo: string | null;
  esquemaNombre: string;
  esquemaPorValidar: boolean;
  aplicadas: number;
  atrasadas: number;
  proximas: number;
  proximaDosis: DosisCarne | null;
  dosis: DosisCarne[];
  registros: RegistroVacuna[];
}

export interface DosisRequest {
  pacienteId: string;
  vacunaId: string;
  numeroDosis: number;
  fechaAplicacion: string;
  lote: string;
  establecimiento?: string | null;
  observaciones?: string | null;
}

export interface PacienteClinicoResumen {
  id: string;
  nombre: string;
  dni: string;
  fechaNacimiento: string | null;
  edadMeses: number | null;
  sexo: string | null;
}

export interface MedicionCrecimiento {
  id: string;
  fecha: string;
  pesoKg: number | null;
  tallaCm: number | null;
  perimetroCefalicoCm: number | null;
  imc: number | null;
  registradoPor: string;
  observaciones: string | null;
}

export interface MedicionRequest {
  pacienteId: string;
  fecha: string;
  pesoKg?: number | null;
  tallaCm?: number | null;
  perimetroCefalicoCm?: number | null;
  observaciones?: string | null;
}

export interface PuntoCrecimiento {
  medicionId: string;
  fecha: string;
  edadMeses: number;
  valor: number;
  z: number | null;
  percentil: number | null;
  nivel: NivelAlerta;
  mensaje: string;
}

export interface ReferenciaPunto {
  edadMeses: number;
  p3: number;
  p15: number;
  p50: number;
  p85: number;
  p97: number;
}

export interface CurvaCrecimiento {
  indicador: IndicadorCrecimiento;
  etiqueta: string;
  unidad: string;
  puntos: PuntoCrecimiento[];
  referencia: ReferenciaPunto[];
}

export interface AlertaCrecimiento {
  indicador: IndicadorCrecimiento;
  etiqueta: string;
  nivel: NivelAlerta;
  mensaje: string;
  z: number;
  fecha: string;
}

export interface Crecimiento {
  pacienteId: string;
  pacienteNombre: string;
  sexo: string | null;
  edadMeses: number | null;
  aplica: boolean;
  referenciaPorValidar: boolean;
  curvas: CurvaCrecimiento[];
  alertas: AlertaCrecimiento[];
  mediciones: MedicionCrecimiento[];
}
