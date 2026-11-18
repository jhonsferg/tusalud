export type TipoComprobante = 'BOLETA' | 'FACTURA';
export type TipoDocumento = 'DNI' | 'RUC' | 'SIN_DOC';
export type MetodoPagoCaja = 'EFECTIVO' | 'TARJETA' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA';
export type OrigenCobro = 'CITA' | 'FARMACIA' | 'LABORATORIO' | 'IMAGEN' | 'SERVICIO' | 'OTRO';

export const METODOS_CAJA: readonly MetodoPagoCaja[] = [
  'EFECTIVO',
  'TARJETA',
  'YAPE',
  'PLIN',
  'TRANSFERENCIA',
];

export const ETIQUETA_METODO_CAJA: Readonly<Record<string, string>> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  YAPE: 'Yape',
  PLIN: 'Plin',
  TRANSFERENCIA: 'Transferencia',
};

export const ETIQUETA_ORIGEN: Readonly<Record<string, string>> = {
  CITA: 'Consulta',
  FARMACIA: 'Farmacia',
  LABORATORIO: 'Laboratorio',
  IMAGEN: 'Imágenes',
  SERVICIO: 'Servicio',
  OTRO: 'Otro',
};

export interface SesionCajaResponse {
  id: string;
  cajeroId: string;
  cajeroNombre: string;
  apertura: string;
  montoApertura: number;
  cierre?: string | null;
  montoContado?: number | null;
  montoEsperado?: number | null;
  diferencia?: number | null;
  estado: 'ABIERTA' | 'CERRADA';
  observaciones?: string | null;
}

export interface CierreCajaRequest {
  montoContado: number;
  observaciones?: string | null;
}

export interface ResumenCajaResponse {
  total: number;
  comprobantes: number;
  porMetodo: { metodo: string; total: number }[];
}

export interface ComprobanteItem {
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface ComprobanteResponse {
  id: string;
  montoCubierto?: number | null;
  aseguradoraNombre?: string | null;
  tipo: TipoComprobante;
  serie: string;
  numero: number;
  codigo: string;
  fecha: string;
  clienteTipoDoc: TipoDocumento;
  clienteDoc?: string | null;
  clienteNombre: string;
  subtotal: number;
  igv: number;
  total: number;
  metodoPago: MetodoPagoCaja;
  estado: 'EMITIDO' | 'ANULADO';
  motivoAnulacion?: string | null;
  origen: OrigenCobro;
  referenciaId?: string | null;
  pacienteId?: string | null;
  cajaId: string;
  cajeroNombre: string;
  items: ComprobanteItem[];
}

export interface CobroRequest {
  forzarParticular?: boolean | null;
  motivoParticular?: string | null;
  origen: OrigenCobro;
  referenciaId?: string | null;
  pacienteId?: string | null;
  tipoComprobante: TipoComprobante;
  clienteTipoDoc: TipoDocumento;
  clienteDoc?: string | null;
  clienteNombre: string;
  metodoPago: MetodoPagoCaja;
  items?: { descripcion: string; cantidad: number; precioUnitario: number }[] | null;
}

export interface PendienteCobroResponse {
  origen: OrigenCobro;
  referenciaId: string;
  paciente: string;
  dni: string;
  descripcion: string;
  monto: number;
  fecha: string;
}

export interface ReporteIngresosResponse {
  total: number;
  porDia: { fecha: string; total: number; comprobantes: number }[];
  porMetodo: { metodo: string; total: number }[];
  porOrigen: { origen: string; total: number }[];
}
