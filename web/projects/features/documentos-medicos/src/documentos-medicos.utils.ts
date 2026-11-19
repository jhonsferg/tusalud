import {
  EstadoConsentimiento,
  EstadoDocumentoMedico,
  EstadoReferencia,
  TipoDocumentoMedico,
} from '@tusalud/core';

export type SeveridadEtiqueta = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

export const SEVERIDAD_ESTADO_DOCUMENTO: Readonly<
  Record<EstadoDocumentoMedico, SeveridadEtiqueta>
> = {
  BORRADOR: 'secondary',
  EMITIDO: 'success',
  ANULADO: 'danger',
};

export const SEVERIDAD_ESTADO_REFERENCIA: Readonly<Record<EstadoReferencia, SeveridadEtiqueta>> = {
  ENVIADA: 'info',
  RECIBIDA: 'warn',
  ATENDIDA: 'success',
  RECHAZADA: 'danger',
};

export const SEVERIDAD_ESTADO_CONSENTIMIENTO: Readonly<
  Record<EstadoConsentimiento, SeveridadEtiqueta>
> = {
  PENDIENTE_FIRMA: 'warn',
  FIRMADO: 'success',
  REVOCADO: 'danger',
  VENCIDO: 'secondary',
};

export type ClaseCampo = 'texto' | 'area' | 'numero' | 'fecha' | 'opcion' | 'establecimiento';

export interface CampoDocumento {
  readonly clave: string;
  readonly etiqueta: string;
  readonly clase: ClaseCampo;
  readonly requerido: boolean;
  readonly maximo?: number;
  readonly opciones?: { label: string; value: string }[];
  readonly ayuda?: string;
}

export const CAMPOS_DOCUMENTO: Readonly<Record<TipoDocumentoMedico, readonly CampoDocumento[]>> = {
  CERTIFICADO: [
    {
      clave: 'motivo',
      etiqueta: 'Motivo del certificado',
      clase: 'area',
      requerido: true,
      maximo: 500,
    },
    { clave: 'hallazgos', etiqueta: 'Hallazgos', clase: 'area', requerido: false, maximo: 1000 },
    {
      clave: 'recomendaciones',
      etiqueta: 'Recomendaciones',
      clase: 'area',
      requerido: false,
      maximo: 1000,
    },
    {
      clave: 'vigenciaDias',
      etiqueta: 'Vigencia en días',
      clase: 'numero',
      requerido: true,
      ayuda: 'Entre 1 y 365 días desde la emisión',
    },
  ],
  DESCANSO: [
    {
      clave: 'tipoDescanso',
      etiqueta: 'Tipo de descanso',
      clase: 'opcion',
      requerido: true,
      opciones: [
        { label: 'Enfermedad', value: 'ENFERMEDAD' },
        { label: 'Accidente', value: 'ACCIDENTE' },
        { label: 'Maternidad', value: 'MATERNIDAD' },
        { label: 'Otros', value: 'OTROS' },
      ],
    },
    { clave: 'desde', etiqueta: 'Desde', clase: 'fecha', requerido: true },
    { clave: 'hasta', etiqueta: 'Hasta', clase: 'fecha', requerido: true },
    {
      clave: 'contingencia',
      etiqueta: 'Contingencia',
      clase: 'texto',
      requerido: true,
      maximo: 200,
    },
    {
      clave: 'indicacion',
      etiqueta: 'Indicación de reposo',
      clase: 'area',
      requerido: true,
      maximo: 500,
    },
  ],
  INFORME: [
    { clave: 'destinatario', etiqueta: 'Dirigido a', clase: 'texto', requerido: true, maximo: 150 },
    { clave: 'resumen', etiqueta: 'Resumen clínico', clase: 'area', requerido: true, maximo: 2000 },
    {
      clave: 'tratamiento',
      etiqueta: 'Tratamiento',
      clase: 'area',
      requerido: false,
      maximo: 1500,
    },
    { clave: 'evolucion', etiqueta: 'Evolución', clase: 'area', requerido: false, maximo: 1500 },
  ],
  REFERENCIA: [
    {
      clave: 'destinoId',
      etiqueta: 'Establecimiento de destino',
      clase: 'establecimiento',
      requerido: true,
    },
    {
      clave: 'especialidad',
      etiqueta: 'Especialidad requerida',
      clase: 'texto',
      requerido: true,
      maximo: 100,
    },
    {
      clave: 'prioridad',
      etiqueta: 'Prioridad',
      clase: 'opcion',
      requerido: true,
      opciones: [
        { label: 'Normal', value: 'NORMAL' },
        { label: 'Preferente', value: 'PREFERENTE' },
        { label: 'Urgente', value: 'URGENTE' },
      ],
    },
    {
      clave: 'motivo',
      etiqueta: 'Motivo de la referencia',
      clase: 'area',
      requerido: true,
      maximo: 500,
    },
    {
      clave: 'resumenClinico',
      etiqueta: 'Resumen clínico',
      clase: 'area',
      requerido: true,
      maximo: 2000,
    },
  ],
  CONTRARREFERENCIA: [
    {
      clave: 'resumen',
      etiqueta: 'Resumen de la atención',
      clase: 'area',
      requerido: true,
      maximo: 2000,
    },
    {
      clave: 'recomendaciones',
      etiqueta: 'Recomendaciones',
      clase: 'area',
      requerido: false,
      maximo: 1000,
    },
  ],
  EPICRISIS: [
    { clave: 'motivo', etiqueta: 'Motivo de ingreso', clase: 'area', requerido: true, maximo: 500 },
    {
      clave: 'diagnosticoIngreso',
      etiqueta: 'Diagnóstico de ingreso',
      clase: 'texto',
      requerido: false,
      maximo: 500,
    },
    {
      clave: 'diagnosticoAlta',
      etiqueta: 'Diagnóstico de alta',
      clase: 'texto',
      requerido: false,
      maximo: 500,
    },
    {
      clave: 'resumenEvolucion',
      etiqueta: 'Resumen de la evolución',
      clase: 'area',
      requerido: true,
      maximo: 2000,
    },
    {
      clave: 'procedimientos',
      etiqueta: 'Procedimientos',
      clase: 'area',
      requerido: false,
      maximo: 1000,
    },
    {
      clave: 'condicionAlta',
      etiqueta: 'Condición de alta',
      clase: 'opcion',
      requerido: true,
      opciones: [
        { label: 'Mejorado', value: 'MEJORADO' },
        { label: 'Estacionario', value: 'ESTACIONARIO' },
        { label: 'Agravado', value: 'AGRAVADO' },
        { label: 'Fallecido', value: 'FALLECIDO' },
      ],
    },
    {
      clave: 'indicaciones',
      etiqueta: 'Indicaciones de alta',
      clase: 'area',
      requerido: true,
      maximo: 1000,
    },
  ],
};

export function valoresIniciales(
  tipo: TipoDocumentoMedico,
  contenido?: Record<string, unknown>,
): Record<string, string> {
  const valores: Record<string, string> = {};
  for (const campo of CAMPOS_DOCUMENTO[tipo]) {
    const actual = contenido?.[campo.clave];
    valores[campo.clave] = actual === undefined || actual === null ? '' : String(actual);
  }
  return valores;
}

export function contenidoDesdeValores(
  tipo: TipoDocumentoMedico,
  valores: Record<string, string>,
): Record<string, unknown> {
  const contenido: Record<string, unknown> = {};
  for (const campo of CAMPOS_DOCUMENTO[tipo]) {
    const valor = (valores[campo.clave] ?? '').trim();
    if (valor) {
      contenido[campo.clave] = campo.clase === 'numero' ? Number(valor) : valor;
    }
  }
  return contenido;
}

export function faltantes(tipo: TipoDocumentoMedico, valores: Record<string, string>): string[] {
  return CAMPOS_DOCUMENTO[tipo]
    .filter((campo) => campo.requerido && !(valores[campo.clave] ?? '').trim())
    .map((campo) => campo.etiqueta);
}

export function fechaIso(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}
