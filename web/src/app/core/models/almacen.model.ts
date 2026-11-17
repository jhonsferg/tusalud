export type TipoProducto = 'MEDICAMENTO' | 'INSUMO' | 'REACTIVO' | 'MATERIAL';
export type EstadoLote = 'VIGENTE' | 'POR_VENCER' | 'VENCIDO' | 'AGOTADO';
export type MotivoSalida = 'CONSUMO_INTERNO' | 'MERMA' | 'VENCIMIENTO' | 'AJUSTE_NEGATIVO';
export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE';

export const TIPOS_PRODUCTO: readonly TipoProducto[] = [
  'MEDICAMENTO',
  'INSUMO',
  'REACTIVO',
  'MATERIAL',
];

export const ETIQUETA_TIPO_PRODUCTO: Readonly<Record<TipoProducto, string>> = {
  MEDICAMENTO: 'Medicamento',
  INSUMO: 'Insumo',
  REACTIVO: 'Reactivo',
  MATERIAL: 'Material',
};

export const ETIQUETA_MOTIVO_SALIDA: Readonly<Record<MotivoSalida, string>> = {
  CONSUMO_INTERNO: 'Consumo interno',
  MERMA: 'Merma',
  VENCIMIENTO: 'Baja por vencimiento',
  AJUSTE_NEGATIVO: 'Ajuste negativo',
};

export interface ProveedorResponse {
  id: number;
  ruc: string;
  razonSocial: string;
  contacto?: string | null;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  activo: boolean;
}

export interface ProveedorRequest {
  ruc: string;
  razonSocial: string;
  contacto?: string | null;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  activo: boolean;
}

export interface ProductoResponse {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  tipo: TipoProducto;
  principioActivo?: string | null;
  presentacion?: string | null;
  unidad: string;
  precioVenta: number;
  stockMinimo: number;
  requiereReceta: boolean;
  activo: boolean;
  stockActual: number;
  proximoVencimiento?: string | null;
  bajoStock: boolean;
}

export interface ProductoRequest {
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  tipo: TipoProducto;
  principioActivo?: string | null;
  presentacion?: string | null;
  unidad: string;
  precioVenta: number;
  stockMinimo: number;
  requiereReceta: boolean;
  activo: boolean;
}

export interface LoteResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  numeroLote: string;
  fechaVencimiento: string;
  cantidadInicial: number;
  cantidadActual: number;
  costoUnitario: number;
  proveedorId?: number | null;
  proveedorNombre?: string | null;
  ubicacion?: string | null;
  estado: EstadoLote;
  createdAt: string;
}

export interface IngresoItemRequest {
  productoId: number;
  numeroLote: string;
  fechaVencimiento: string;
  cantidad: number;
  costoUnitario: number;
  ubicacion?: string | null;
}

export interface IngresoRequest {
  proveedorId?: number | null;
  documento?: string | null;
  observaciones?: string | null;
  items: IngresoItemRequest[];
}

export interface IngresoResponse {
  id: number;
  fecha: string;
  documento?: string | null;
  proveedorNombre?: string | null;
  observaciones?: string | null;
  total: number;
  usuarioNombre: string;
  items: LoteResponse[];
}

export interface SalidaRequest {
  productoId: number;
  cantidad: number;
  motivo: MotivoSalida;
  observaciones?: string | null;
}

export interface AjusteRequest {
  loteId: number;
  cantidad: number;
  observaciones?: string | null;
}

export interface MovimientoResponse {
  id: number;
  fecha: string;
  tipo: TipoMovimiento;
  motivo: string;
  productoId: number;
  productoNombre: string;
  loteId?: number | null;
  numeroLote?: string | null;
  cantidad: number;
  usuarioNombre: string;
  referencia?: string | null;
}

export interface AlertasAlmacenResponse {
  bajoStock: ProductoResponse[];
  porVencer: LoteResponse[];
  vencidos: LoteResponse[];
}

export interface ResumenAlmacenResponse {
  totalProductos: number;
  productosBajoStock: number;
  lotesPorVencer: number;
  lotesVencidos: number;
  valorInventario: number;
}

export interface FiltroMovimientos {
  productoId?: number | null;
  tipo?: TipoMovimiento | null;
  desde?: string | null;
  hasta?: string | null;
}
