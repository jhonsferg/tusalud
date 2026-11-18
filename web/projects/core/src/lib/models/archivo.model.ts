export type PropositoArchivo =
  'AVATAR' | 'IMAGEN_DIAGNOSTICA' | 'DOCUMENTO_PACIENTE' | 'REPORTE' | 'PLANTILLA';

export interface ArchivoResponse {
  id: string;
  nombre: string;
  tipoContenido: string;
  tamano: number;
  sha256: string;
  proposito: PropositoArchivo;
  referenciaTipo?: string | null;
  referenciaId?: string | null;
  createdAt: string;
}

export interface ReglasArchivo {
  readonly tipos: readonly string[];
  readonly extensiones: readonly string[];
  readonly maxBytes: number;
  readonly maxArchivos: number;
}

const MB = 1024 * 1024;

export const REGLAS_ARCHIVO: Readonly<Record<PropositoArchivo, ReglasArchivo>> = {
  AVATAR: {
    tipos: ['image/jpeg', 'image/png', 'image/webp'],
    extensiones: ['jpg', 'jpeg', 'png', 'webp'],
    maxBytes: 2 * MB,
    maxArchivos: 1,
  },
  IMAGEN_DIAGNOSTICA: {
    tipos: ['image/jpeg', 'image/png', 'application/pdf'],
    extensiones: ['jpg', 'jpeg', 'png', 'pdf'],
    maxBytes: 5 * MB,
    maxArchivos: 12,
  },
  DOCUMENTO_PACIENTE: {
    tipos: [
      'application/pdf',
      'image/jpeg',
      'image/png',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'text/csv',
    ],
    extensiones: ['pdf', 'jpg', 'jpeg', 'png', 'docx', 'xlsx', 'csv'],
    maxBytes: 10 * MB,
    maxArchivos: 10,
  },
  REPORTE: {
    tipos: [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'text/csv',
    ],
    extensiones: ['pdf', 'xlsx', 'docx', 'csv'],
    maxBytes: 25 * MB,
    maxArchivos: 1,
  },
  PLANTILLA: {
    tipos: [
      'application/xml',
      'text/xml',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    ],
    extensiones: ['jrxml', 'xml', 'xlsx', 'docx'],
    maxBytes: 2 * MB,
    maxArchivos: 1,
  },
};

export function extensionDe(nombre: string): string {
  const punto = nombre.lastIndexOf('.');
  return punto < 0 || punto === nombre.length - 1 ? '' : nombre.slice(punto + 1).toLowerCase();
}

export function formatoTamano(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function validarArchivo(archivo: File, reglas: ReglasArchivo): string | null {
  if (archivo.size === 0) {
    return `${archivo.name} está vacío.`;
  }
  if (archivo.size > reglas.maxBytes) {
    return `${archivo.name} supera el máximo de ${formatoTamano(reglas.maxBytes)}.`;
  }
  if (!reglas.extensiones.includes(extensionDe(archivo.name))) {
    return `${archivo.name} no tiene un formato permitido: ${reglas.extensiones.join(', ')}.`;
  }
  return null;
}

export interface ProgresoSubida {
  readonly progreso: number;
  readonly archivo: ArchivoResponse | null;
}
