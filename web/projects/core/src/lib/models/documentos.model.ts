export type TipoDocumentoPdf =
  | 'comprobante'
  | 'receta'
  | 'resultados-laboratorio'
  | 'informe-imagen'
  | 'constancia'
  | 'resumen-historia';

export type TipoExportacion =
  | 'reporte-ejecutivo'
  | 'reporte-financiero'
  | 'reporte-operativo'
  | 'reporte-calidad'
  | 'listado-comprobantes'
  | 'kardex'
  | 'stock'
  | 'ausencias'
  | 'turnos'
  | 'mis-citas';

export type FormatoExportacion = 'PDF' | 'XLSX';

export type EstadoExportacion = 'SOLICITADA' | 'PROCESANDO' | 'LISTA' | 'ERROR' | 'EXPIRADA';

export const ETIQUETA_TIPO_EXPORTACION: Readonly<Record<string, string>> = {
  'reporte-ejecutivo': 'Reporte ejecutivo',
  'reporte-financiero': 'Reporte financiero',
  'reporte-operativo': 'Reporte operativo',
  'reporte-calidad': 'Reporte de calidad',
  'listado-comprobantes': 'Listado de comprobantes',
  kardex: 'Kardex del almacén',
  stock: 'Stock del almacén',
  ausencias: 'Ausencias del personal',
  turnos: 'Turnos del personal',
  'mis-citas': 'Mis citas',
};

export const ETIQUETA_ESTADO_EXPORTACION: Readonly<Record<EstadoExportacion, string>> = {
  SOLICITADA: 'En cola',
  PROCESANDO: 'Procesando',
  LISTA: 'Lista',
  ERROR: 'Con error',
  EXPIRADA: 'Expirada',
};

export type ParametrosExportacion = Readonly<
  Record<string, string | number | boolean | null | undefined>
>;

export interface ExportacionRequest {
  tipo: TipoExportacion;
  formato: FormatoExportacion;
  parametros?: Record<string, string>;
}

export interface ExportacionResponse {
  id: string;
  tipo: string;
  formato: FormatoExportacion;
  estado: EstadoExportacion;
  nombre: string | null;
  tamano: number | null;
  error: string | null;
  creadaEn: string;
  terminadaEn: string | null;
  expiraEn: string | null;
}

export interface PlantillaResponse {
  codigo: string;
  nombre: string;
  version: number;
  personalizada: boolean;
  actualizadaPor: string | null;
  actualizadaEn: string | null;
  motivo: string | null;
}

export interface PlantillaVersionResponse {
  version: number;
  activa: boolean;
  restauracion: boolean;
  motivo: string | null;
  creadaPor: string;
  creadaEn: string;
}

export interface ArchivoDescargado {
  blob: Blob;
  nombre: string;
}
