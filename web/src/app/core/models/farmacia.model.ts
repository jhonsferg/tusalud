export type MetodoPagoFarmacia = 'EFECTIVO' | 'TARJETA' | 'YAPE' | 'PLIN';
export type EstadoReceta = 'PENDIENTE' | 'PARCIAL' | 'DISPENSADA';
export type EstadoDispensacion = 'EMITIDA' | 'ANULADA';

export const METODOS_PAGO_FARMACIA: readonly MetodoPagoFarmacia[] = [
  'EFECTIVO',
  'TARJETA',
  'YAPE',
  'PLIN',
];

export const ETIQUETA_METODO_FARMACIA: Readonly<Record<MetodoPagoFarmacia, string>> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  YAPE: 'Yape',
  PLIN: 'Plin',
};

export interface CatalogoProductoResponse {
  id: number;
  codigo: string;
  nombre: string;
  principioActivo?: string | null;
  presentacion?: string | null;
  unidad: string;
  precioVenta: number;
  stockActual: number;
  requiereReceta: boolean;
}

export interface RecetaItemFarmaciaResponse {
  id: number;
  medicamento: string;
  dosis?: string | null;
  frecuencia?: string | null;
  duracion?: string | null;
  indicaciones?: string | null;
  dispensado: boolean;
}

export interface RecetaFarmaciaResponse {
  atencionId: number;
  citaId: number;
  fecha: string;
  pacienteId: number;
  pacienteNombre: string;
  dni: string;
  medicoNombre: string;
  estado: EstadoReceta;
  items: RecetaItemFarmaciaResponse[];
}

export interface DispensacionItemRequest {
  recetaItemId?: number | null;
  productoId: number;
  cantidad: number;
}

export interface DispensacionRequest {
  atencionId?: number | null;
  pacienteId?: number | null;
  items: DispensacionItemRequest[];
  metodoPago: MetodoPagoFarmacia;
  observaciones?: string | null;
}

export interface DispensacionItemResponse {
  productoId: number;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  recetaItemId?: number | null;
}

export interface DispensacionResponse {
  id: number;
  fecha: string;
  comprobante: string;
  estado: EstadoDispensacion;
  pacienteId?: number | null;
  pacienteNombre: string;
  farmaceuticoNombre: string;
  metodoPago: MetodoPagoFarmacia;
  total: number;
  observaciones?: string | null;
  items: DispensacionItemResponse[];
}

export interface ResumenFarmaciaResponse {
  ventasHoy: number;
  dispensacionesHoy: number;
  recetasPendientes: number;
}
