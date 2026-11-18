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
  id: string;
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
  id: string;
  medicamento: string;
  dosis?: string | null;
  frecuencia?: string | null;
  duracion?: string | null;
  indicaciones?: string | null;
  dispensado: boolean;
}

export interface RecetaFarmaciaResponse {
  atencionId: string;
  citaId: string;
  fecha: string;
  pacienteId: string;
  pacienteNombre: string;
  dni: string;
  medicoNombre: string;
  estado: EstadoReceta;
  items: RecetaItemFarmaciaResponse[];
}

export interface DispensacionItemRequest {
  recetaItemId?: string | null;
  productoId: string;
  cantidad: number;
}

export interface DispensacionRequest {
  atencionId?: string | null;
  pacienteId?: string | null;
  items: DispensacionItemRequest[];
  metodoPago: MetodoPagoFarmacia;
  observaciones?: string | null;
}

export interface DispensacionItemResponse {
  productoId: string;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  recetaItemId?: string | null;
}

export interface DispensacionResponse {
  id: string;
  fecha: string;
  comprobante: string;
  estado: EstadoDispensacion;
  pacienteId?: string | null;
  pacienteNombre: string;
  farmaceuticoNombre: string;
  metodoPago: MetodoPagoFarmacia;
  total: number;
  observaciones?: string | null;
  items: DispensacionItemResponse[];
  motivoAnulacion?: string | null;
}

export interface ResumenFarmaciaResponse {
  ventasHoy: number;
  dispensacionesHoy: number;
  recetasPendientes: number;
}
