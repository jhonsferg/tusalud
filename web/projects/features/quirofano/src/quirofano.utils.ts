import {
  CirugiaResumen,
  EstadoCirugia,
  EstadoConsentimientoCirugia,
  EstadoQuirofano,
  MomentoChecklist,
  RolQuirurgico,
  TipoAnestesia,
  TipoCirugia,
} from '@tusalud/core';
import { CalEvento, ColorEvento } from '@tusalud/ui/calendario';

export const ETIQUETA_ESTADO_CIRUGIA: Readonly<Record<EstadoCirugia, string>> = {
  SOLICITADA: 'Solicitada',
  PROGRAMADA: 'Programada',
  EN_PREPARACION: 'En preparación',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  SUSPENDIDA: 'Suspendida',
  CANCELADA: 'Cancelada',
};

export const COLOR_ESTADO_CIRUGIA: Readonly<Record<EstadoCirugia, ColorEvento>> = {
  SOLICITADA: 'secondary',
  PROGRAMADA: 'info',
  EN_PREPARACION: 'warn',
  EN_CURSO: 'purple',
  FINALIZADA: 'success',
  SUSPENDIDA: 'warn',
  CANCELADA: 'secondary',
};

export type SeveridadTag = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

export const SEVERIDAD_ESTADO_CIRUGIA: Readonly<Record<EstadoCirugia, SeveridadTag>> = {
  SOLICITADA: 'secondary',
  PROGRAMADA: 'info',
  EN_PREPARACION: 'warn',
  EN_CURSO: 'contrast',
  FINALIZADA: 'success',
  SUSPENDIDA: 'warn',
  CANCELADA: 'danger',
};

export const ETIQUETA_TIPO_CIRUGIA: Readonly<Record<TipoCirugia, string>> = {
  ELECTIVA: 'Electiva',
  URGENCIA: 'Urgencia',
};

export const ETIQUETA_ANESTESIA: Readonly<Record<TipoAnestesia, string>> = {
  GENERAL: 'General',
  REGIONAL: 'Regional',
  LOCAL: 'Local',
  SEDACION: 'Sedación',
  NINGUNA: 'Ninguna',
};

export const ETIQUETA_ROL: Readonly<Record<RolQuirurgico, string>> = {
  CIRUJANO: 'Cirujano',
  AYUDANTE: 'Ayudante',
  ANESTESIOLOGO: 'Anestesiólogo',
  INSTRUMENTISTA: 'Instrumentista',
  CIRCULANTE: 'Circulante',
};

export const ETIQUETA_QUIROFANO: Readonly<Record<EstadoQuirofano, string>> = {
  DISPONIBLE: 'Disponible',
  MANTENIMIENTO: 'En mantenimiento',
  FUERA_DE_SERVICIO: 'Fuera de servicio',
};

export const ETIQUETA_CONSENTIMIENTO: Readonly<Record<EstadoConsentimientoCirugia, string>> = {
  PENDIENTE_FIRMA: 'Pendiente de firma',
  FIRMADO: 'Firmado',
  REVOCADO: 'Revocado',
  VENCIDO: 'Vencido',
};

export const ETIQUETA_MOMENTO: Readonly<Record<MomentoChecklist, string>> = {
  ANTES_INDUCCION: 'Antes de la inducción anestésica',
  ANTES_INCISION: 'Antes de la incisión',
  ANTES_SALIDA: 'Antes de salir del quirófano',
};

export const MOMENTOS: readonly MomentoChecklist[] = [
  'ANTES_INDUCCION',
  'ANTES_INCISION',
  'ANTES_SALIDA',
];

export const ITEMS_CHECKLIST: Readonly<Record<MomentoChecklist, readonly string[]>> = {
  ANTES_INDUCCION: [
    'IDENTIDAD_CONFIRMADA',
    'SITIO_MARCADO',
    'CONSENTIMIENTOS_VERIFICADOS',
    'EQUIPO_ANESTESIA_REVISADO',
    'ALERGIAS_REVISADAS',
  ],
  ANTES_INCISION: [
    'EQUIPO_SE_PRESENTO',
    'PROCEDIMIENTO_CONFIRMADO',
    'ANTIBIOTICO_ADMINISTRADO',
    'ESTERILIDAD_CONFIRMADA',
    'IMAGENES_DISPONIBLES',
  ],
  ANTES_SALIDA: [
    'PROCEDIMIENTO_REGISTRADO',
    'CONTEO_INSTRUMENTAL_COMPLETO',
    'MUESTRAS_ETIQUETADAS',
    'PROBLEMAS_DEL_EQUIPO_REVISADOS',
    'PLAN_POSTOPERATORIO_DEFINIDO',
  ],
};

export const ETIQUETA_ITEM: Readonly<Record<string, string>> = {
  IDENTIDAD_CONFIRMADA: 'Identidad del paciente, procedimiento y lateralidad confirmados',
  SITIO_MARCADO: 'Sitio quirúrgico marcado',
  CONSENTIMIENTOS_VERIFICADOS: 'Consentimientos firmados y verificados',
  EQUIPO_ANESTESIA_REVISADO: 'Equipo y medicación de anestesia revisados',
  ALERGIAS_REVISADAS: 'Alergias y riesgo de vía aérea revisados',
  EQUIPO_SE_PRESENTO: 'Todo el equipo se presentó por su nombre y función',
  PROCEDIMIENTO_CONFIRMADO: 'Paciente, sitio y procedimiento confirmados en voz alta',
  ANTIBIOTICO_ADMINISTRADO: 'Profilaxis antibiótica administrada si corresponde',
  ESTERILIDAD_CONFIRMADA: 'Esterilidad del instrumental confirmada',
  IMAGENES_DISPONIBLES: 'Imágenes diagnósticas disponibles',
  PROCEDIMIENTO_REGISTRADO: 'Nombre del procedimiento registrado',
  CONTEO_INSTRUMENTAL_COMPLETO: 'Conteo de instrumental, gasas y agujas completo',
  MUESTRAS_ETIQUETADAS: 'Muestras etiquetadas correctamente',
  PROBLEMAS_DEL_EQUIPO_REVISADOS: 'Problemas del equipo revisados',
  PLAN_POSTOPERATORIO_DEFINIDO: 'Plan de recuperación y manejo postoperatorio definido',
};

export const ROLES_MEDICOS: readonly RolQuirurgico[] = ['CIRUJANO', 'AYUDANTE', 'ANESTESIOLOGO'];

export function esRolMedico(rol: RolQuirurgico): boolean {
  return ROLES_MEDICOS.includes(rol);
}

export function tituloEvento(c: CirugiaResumen): string {
  const lugar = c.quirofanoNombre ?? 'Sin quirófano';
  return c.detalleVisible && c.procedimiento
    ? `${lugar} · ${c.procedimiento}`
    : `${lugar} · Reservado`;
}

export function cirugiaAEvento(c: CirugiaResumen): CalEvento | null {
  if (!c.inicio || !c.fin) {
    return null;
  }
  return {
    id: c.id,
    inicio: new Date(c.inicio),
    fin: new Date(c.fin),
    titulo: tituloEvento(c),
    detalle: `${c.codigo} · ${ETIQUETA_ESTADO_CIRUGIA[c.estado]}`,
    color: COLOR_ESTADO_CIRUGIA[c.estado],
    categoria: c.estado,
    ignoraCruce: true,
  };
}

export function fechaHora(valor: string | null | undefined): string {
  if (!valor) {
    return '—';
  }
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(valor));
}

export function aIsoLocal(fecha: Date): string {
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}:00`;
}

export function faltantesChecklist(
  momento: MomentoChecklist,
  marcados: ReadonlySet<string>,
): string[] {
  return ITEMS_CHECKLIST[momento].filter((item) => !marcados.has(item));
}

export function puedeProgramarse(estado: EstadoCirugia): boolean {
  return estado === 'SOLICITADA' || estado === 'SUSPENDIDA' || estado === 'PROGRAMADA';
}

export function puedeCancelarse(estado: EstadoCirugia): boolean {
  return (
    estado === 'SOLICITADA' ||
    estado === 'PROGRAMADA' ||
    estado === 'EN_PREPARACION' ||
    estado === 'SUSPENDIDA'
  );
}

export function puedeSuspenderse(estado: EstadoCirugia): boolean {
  return estado === 'PROGRAMADA' || estado === 'EN_PREPARACION' || estado === 'EN_CURSO';
}

export function momentoDisponible(
  momento: MomentoChecklist,
  estado: EstadoCirugia,
  completados: ReadonlySet<MomentoChecklist>,
): boolean {
  if (completados.has(momento)) {
    return false;
  }
  if (momento === 'ANTES_INDUCCION') {
    return estado === 'EN_PREPARACION' || estado === 'EN_CURSO';
  }
  if (momento === 'ANTES_INCISION') {
    return estado === 'EN_CURSO' && completados.has('ANTES_INDUCCION');
  }
  return estado === 'EN_CURSO' && completados.has('ANTES_INCISION');
}
