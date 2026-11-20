export type CampoCorregible = 'DIAGNOSTICO' | 'TRATAMIENTO' | 'INDICACIONES' | 'OBSERVACIONES';

export interface CierreAtencion {
  cerrada: boolean;
  atencionId: string;
  cerradoEn: string | null;
  cerradoPor: string | null;
  hashCorto: string | null;
  firmaValida: boolean;
  certificadoDePrueba: boolean;
  advertencia: string | null;
}

export interface ContenidoClinico {
  diagnostico: string | null;
  tratamiento: string | null;
  indicaciones: string | null;
  observaciones: string | null;
}

export interface VersionAtencion {
  id: string;
  numero: number;
  tipo: 'ADENDA' | 'CORRECCION';
  campo: CampoCorregible | null;
  valorAnterior: string | null;
  valorNuevo: string;
  motivo: string;
  autor: string;
  creadoEn: string;
}

export interface HistorialAtencion {
  atencionId: string;
  fecha: string;
  cerrada: boolean;
  original: ContenidoClinico;
  vigente: ContenidoClinico;
  versiones: VersionAtencion[];
  cierre: CierreAtencion;
}

export interface AdendaRequest {
  texto: string;
  motivo: string;
}

export interface CorreccionRequest {
  campo: CampoCorregible;
  valorNuevo: string;
  motivo: string;
}

export interface PoliticaConservacion {
  id: string;
  tipoRegistro: string;
  descripcion: string;
  aniosMinimos: number;
  referencia: string | null;
  porConfirmar: boolean;
  depuracionBloqueada: boolean;
  actualizadoEn: string;
}

export interface PoliticaConservacionRequest {
  aniosMinimos: number;
  referencia: string | null;
  porConfirmar: boolean;
}

export interface PacienteDuplicado {
  pacienteAId: string;
  nombreA: string;
  dniA: string;
  historialA: string;
  pacienteBId: string;
  nombreB: string;
  dniB: string;
  historialB: string;
  criterio: string;
}

export interface FusionPacientes {
  id: string;
  destinoId: string;
  destinoNombre: string;
  origenHistorial: string;
  origenDni: string;
  origenNombre: string;
  motivo: string;
  resumen: string;
  realizadoPor: string;
  realizadoEn: string;
}

export interface FusionPacientesRequest {
  origenId: string;
  destinoId: string;
  motivo: string;
}
