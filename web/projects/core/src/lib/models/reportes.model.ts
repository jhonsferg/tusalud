import type { SegurosReporteResponse } from './seguros.model';
export type Agrupacion = 'DIA' | 'SEMANA' | 'MES';

export const AGRUPACIONES: readonly { label: string; value: Agrupacion }[] = [
  { label: 'Por día', value: 'DIA' },
  { label: 'Por semana', value: 'SEMANA' },
  { label: 'Por mes', value: 'MES' },
];

export interface FiltroReporte {
  desde: string;
  hasta: string;
  agrupacion: Agrupacion;
}

export interface PeriodoReporte {
  desde: string;
  hasta: string;
  agrupacion: Agrupacion;
  dias: number;
}

export interface Indicador {
  actual: number;
  anterior: number;
  variacionPct?: number | null;
}

export interface PuntoEjecutivo {
  fecha: string;
  citas: number;
  atendidas: number;
  ingresos: number;
}

export interface ReporteEjecutivoResponse {
  periodo: PeriodoReporte;
  periodoAnterior: PeriodoReporte;
  citas: Indicador;
  atendidas: Indicador;
  canceladas: Indicador;
  tasaAusenciaPct: Indicador;
  ingresos: Indicador;
  ticketPromedio: Indicador;
  ocupacionHospitalariaPct: Indicador;
  esperaPromedioMin: Indicador;
  valoracionPromedio: Indicador;
  serie: PuntoEjecutivo[];
}

export interface TotalPorDia {
  fecha: string;
  total: number;
  transacciones: number;
}

export interface TotalPorMetodo {
  metodo: string;
  total: number;
  transacciones: number;
}

export interface TotalPorOrigen {
  origen: string;
  total: number;
  devengado: boolean;
}

export interface TotalPorNombre {
  nombre: string;
  cantidad: number;
  total: number;
}

export interface CajaPorCajero {
  nombre: string;
  sesiones: number;
  faltante: number;
  sobrante: number;
  neto: number;
}

export interface PorCobrarDetalle {
  cantidad: number;
  monto: number;
}

export interface ReporteFinancieroResponse {
  seguros?: SegurosReporteResponse;
  periodo: PeriodoReporte;
  totalCobrado: number;
  transacciones: number;
  ticketPromedio: number;
  porDia: TotalPorDia[];
  porMetodo: TotalPorMetodo[];
  porOrigen: TotalPorOrigen[];
  porEspecialidad: TotalPorNombre[];
  porMedico: TotalPorNombre[];
  comprobantes: { emitidos: number; anulados: number; tasaAnulacionPct: number };
  cajas: {
    sesionesCerradas: number;
    faltante: number;
    sobrante: number;
    neto: number;
    porCajero: CajaPorCajero[];
  };
  porCobrar: { citas: PorCobrarDetalle; laboratorio: PorCobrarDetalle; total: number };
}

export interface CamasPorArea {
  area: string;
  total: number;
  ocupadas: number;
  libres: number;
  limpieza: number;
  mantenimiento: number;
  porcentaje: number;
}

export interface EsperaPorHora {
  hora: number;
  turnos: number;
  promedioMin: number;
}

export interface DemandaCelda {
  diaSemana: number;
  hora: number;
  citas: number;
}

export interface UtilizacionAgenda {
  medico: string;
  citas: number;
  capacidad: number;
  utilizacionPct: number;
}

export interface ProductoCritico {
  codigo: string;
  nombre: string;
  stock: number;
  stockMinimo: number;
}

export interface ReporteOperativoResponse {
  periodo: PeriodoReporte;
  ocupacionPromedioPct: number;
  camasActuales: CamasPorArea[];
  ocupacionSerie: { fecha: string; porcentaje: number }[];
  altas: number;
  estanciaPromedioDias: number;
  esperas: { turnos: number; promedioMin: number; p90Min: number; porHora: EsperaPorHora[] };
  emergenciasPorNivel: { nivel: number; clasificacion: string; cantidad: number }[];
  demanda: DemandaCelda[];
  agenda: UtilizacionAgenda[];
  inventario: {
    bajoMinimo: ProductoCritico[];
    lotesPorVencer: number;
    unidadesPorVencer: number;
    lotesVencidos: number;
    unidadesVencidas: number;
  };
}

export interface ValoracionResumen {
  nombre: string;
  total: number;
  promedio: number;
  distribucion: number[];
}

export interface CitasPorEspecialidad {
  especialidad: string;
  total: number;
  canceladas: number;
  noAsistieron: number;
  tasaCancelacionPct: number;
  tasaAusenciaPct: number;
}

export interface MotivoAnulacion {
  motivo: string;
  cantidad: number;
}

export interface ReporteCalidadResponse {
  periodo: PeriodoReporte;
  valoraciones: { promedio: number; total: number; distribucion: number[] };
  porMedico: ValoracionResumen[];
  porEspecialidad: ValoracionResumen[];
  citasPorEspecialidad: CitasPorEspecialidad[];
  laboratorio: {
    completadas: number;
    canceladas: number;
    entregaPromedioHoras: number;
    entregaP90Horas: number;
    porPrioridad: { prioridad: string; cantidad: number; promedioHoras: number }[];
  };
  anulaciones: {
    comprobantes: number;
    dispensaciones: number;
    motivosComprobantes: MotivoAnulacion[];
    motivosDispensaciones: MotivoAnulacion[];
  };
}

export const ETIQUETA_ORIGEN_INGRESO: Readonly<Record<string, string>> = {
  CONSULTA: 'Consultas',
  FARMACIA: 'Farmacia',
  LABORATORIO: 'Laboratorio',
  IMAGENES: 'Imágenes',
  SERVICIOS: 'Servicios',
  HOSPITALIZACION: 'Hospitalización',
};

export const NOMBRES_DIAS_SEMANA: readonly string[] = [
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
  'Domingo',
];
