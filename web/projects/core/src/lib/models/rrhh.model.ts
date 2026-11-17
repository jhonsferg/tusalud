export type AreaLaboral =
  | 'MEDICA'
  | 'ENFERMERIA'
  | 'LABORATORIO'
  | 'IMAGENES'
  | 'FARMACIA'
  | 'ALMACEN'
  | 'RECEPCION'
  | 'CAJA'
  | 'ADMINISTRACION'
  | 'FINANZAS'
  | 'OPERACIONES'
  | 'RRHH'
  | 'CALIDAD'
  | 'GERENCIA';

export const AREAS_LABORALES: readonly AreaLaboral[] = [
  'MEDICA',
  'ENFERMERIA',
  'LABORATORIO',
  'IMAGENES',
  'FARMACIA',
  'ALMACEN',
  'RECEPCION',
  'CAJA',
  'ADMINISTRACION',
  'FINANZAS',
  'OPERACIONES',
  'RRHH',
  'CALIDAD',
  'GERENCIA',
];

export const ETIQUETA_AREA_LABORAL: Readonly<Record<string, string>> = {
  MEDICA: 'Médica',
  ENFERMERIA: 'Enfermería',
  LABORATORIO: 'Laboratorio',
  IMAGENES: 'Imágenes diagnósticas',
  FARMACIA: 'Farmacia',
  ALMACEN: 'Almacén',
  RECEPCION: 'Recepción',
  CAJA: 'Caja',
  ADMINISTRACION: 'Administración',
  FINANZAS: 'Finanzas',
  OPERACIONES: 'Operaciones',
  RRHH: 'Recursos humanos',
  CALIDAD: 'Calidad',
  GERENCIA: 'Gerencia',
};

export type TipoContrato = 'INDEFINIDO' | 'PLAZO_FIJO' | 'PART_TIME' | 'PRACTICAS' | 'LOCACION';

export const TIPOS_CONTRATO: readonly TipoContrato[] = [
  'INDEFINIDO',
  'PLAZO_FIJO',
  'PART_TIME',
  'PRACTICAS',
  'LOCACION',
];

export const ETIQUETA_CONTRATO: Readonly<Record<string, string>> = {
  INDEFINIDO: 'Indefinido',
  PLAZO_FIJO: 'Plazo fijo',
  PART_TIME: 'Tiempo parcial',
  PRACTICAS: 'Prácticas',
  LOCACION: 'Locación de servicios',
};

export type EstadoEmpleado = 'ACTIVO' | 'LICENCIA' | 'CESADO';

export const ESTADOS_EMPLEADO: readonly EstadoEmpleado[] = ['ACTIVO', 'LICENCIA', 'CESADO'];

export const ETIQUETA_ESTADO_EMPLEADO: Readonly<Record<string, string>> = {
  ACTIVO: 'Activo',
  LICENCIA: 'En licencia',
  CESADO: 'Cesado',
};

export type TipoAusencia = 'VACACIONES' | 'PERMISO' | 'DESCANSO_MEDICO' | 'LICENCIA' | 'OTRO';

export const TIPOS_AUSENCIA: readonly TipoAusencia[] = [
  'VACACIONES',
  'PERMISO',
  'DESCANSO_MEDICO',
  'LICENCIA',
  'OTRO',
];

export const ETIQUETA_TIPO_AUSENCIA: Readonly<Record<string, string>> = {
  VACACIONES: 'Vacaciones',
  PERMISO: 'Permiso',
  DESCANSO_MEDICO: 'Descanso médico',
  LICENCIA: 'Licencia',
  OTRO: 'Otro',
};

export type EstadoAusencia = 'SOLICITADA' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA';

export const ESTADOS_AUSENCIA: readonly EstadoAusencia[] = [
  'SOLICITADA',
  'APROBADA',
  'RECHAZADA',
  'CANCELADA',
];

export const ETIQUETA_ESTADO_AUSENCIA: Readonly<Record<string, string>> = {
  SOLICITADA: 'Solicitada',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  CANCELADA: 'Cancelada',
};

export interface EmpleadoResponse {
  id: string;
  usuarioId: string;
  nombreCompleto: string;
  email: string;
  roles: string[];
  cargo: string;
  area: AreaLaboral;
  tipoContrato: TipoContrato;
  fechaIngreso: string;
  fechaFinContrato?: string | null;
  estado: EstadoEmpleado;
  sueldo?: number | null;
  contactoEmergenciaNombre?: string | null;
  contactoEmergenciaTelefono?: string | null;
  diasVacacionesAnuales: number;
}

export interface EmpleadoRequest {
  usuarioId?: string | null;
  cargo: string;
  area: AreaLaboral;
  tipoContrato: TipoContrato;
  fechaIngreso: string;
  fechaFinContrato?: string | null;
  estado: EstadoEmpleado;
  sueldo: number;
  contactoEmergenciaNombre?: string | null;
  contactoEmergenciaTelefono?: string | null;
  diasVacacionesAnuales?: number | null;
}

export interface UsuarioDisponibleResponse {
  usuarioId: string;
  nombreCompleto: string;
  email: string;
  roles: string[];
}

export interface FiltroEmpleados {
  q?: string | null;
  area?: string | null;
  estado?: string | null;
}

export interface ContratoPorVencer {
  empleadoId: string;
  nombreCompleto: string;
  cargo: string;
  fechaFin: string;
  diasRestantes: number;
}

export interface AusenteHoy {
  empleadoId: string;
  nombreCompleto: string;
  area: AreaLaboral;
  tipo: TipoAusencia;
  hasta: string;
}

export interface ResumenRrhhResponse {
  totalEmpleados: number;
  activos: number;
  enLicencia: number;
  cesados: number;
  solicitudesPendientes: number;
  masaSalarial?: number | null;
  porArea: { area: AreaLaboral; total: number }[];
  contratosPorVencer: ContratoPorVencer[];
  ausentesHoy: number;
  ausentesHoyDetalle?: AusenteHoy[] | null;
}

export interface PlantillaTurnoResponse {
  id: string;
  nombre: string;
  horaInicio: string;
  horaFin: string;
  horas: number;
  cruzaMedianoche: boolean;
}

export interface TurnoPersonalResponse {
  id: string;
  empleadoId: string;
  empleadoNombre: string;
  area: AreaLaboral;
  plantillaId: string;
  plantillaNombre: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  horas: number;
  notas?: string | null;
}

export interface TurnoPersonalRequest {
  empleadoId: string;
  plantillaId: string;
  fecha: string;
  notas?: string | null;
}

export interface DiaTurnos {
  fecha: string;
  turnos: TurnoPersonalResponse[];
  ausencia?: AusenciaResponse | null;
}

export interface FilaSemana {
  empleadoId: string;
  nombreCompleto: string;
  cargo: string;
  area: AreaLaboral;
  horasSemana: number;
  dias: DiaTurnos[];
}

export interface SemanaTurnosResponse {
  desde: string;
  hasta: string;
  area?: AreaLaboral | null;
  filas: FilaSemana[];
}

export interface AusenciaResponse {
  id: string;
  empleadoId: string;
  empleadoNombre: string;
  area: AreaLaboral;
  tipo: TipoAusencia;
  fechaInicio: string;
  fechaFin: string;
  dias: number;
  motivo?: string | null;
  estado: EstadoAusencia;
  resueltoPor?: string | null;
  resueltoEn?: string | null;
  motivoResolucion?: string | null;
  createdAt: string;
}

export interface AusenciaRequest {
  tipo: TipoAusencia;
  fechaInicio: string;
  fechaFin: string;
  motivo?: string | null;
}

export interface FiltroAusencias {
  estado?: string | null;
  area?: string | null;
  desde?: string | null;
  hasta?: string | null;
}

export interface SaldoVacacionesResponse {
  empleadoId: string;
  anio: number;
  diasAnuales: number;
  tomados: number;
  pendientesAprobacion: number;
  saldo: number;
}

export interface MisAusenciasResponse {
  saldo: SaldoVacacionesResponse;
  ausencias: AusenciaResponse[];
}
