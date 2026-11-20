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
  id: string;
  ruc: string;
  razonSocial: string;
  contacto?: string | null;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  activo: boolean;
  condicionPagoDias: number;
  plazoEntregaDias?: number | null;
  banco?: string | null;
  cuentaBancaria?: string | null;
  notas?: string | null;
}

export interface ProveedorRequest {
  ruc: string;
  razonSocial: string;
  contacto?: string | null;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  activo: boolean;
  condicionPagoDias?: number | null;
  plazoEntregaDias?: number | null;
  banco?: string | null;
  cuentaBancaria?: string | null;
  notas?: string | null;
}

export interface ProductoResponse {
  id: string;
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
  id: string;
  productoId: string;
  productoNombre: string;
  numeroLote: string;
  fechaVencimiento: string;
  cantidadInicial: number;
  cantidadActual: number;
  costoUnitario: number;
  proveedorId?: string | null;
  proveedorNombre?: string | null;
  ubicacion?: string | null;
  estado: EstadoLote;
  createdAt: string;
}

export interface IngresoItemRequest {
  productoId: string;
  numeroLote: string;
  fechaVencimiento: string;
  cantidad: number;
  costoUnitario: number;
  ubicacion?: string | null;
}

export interface IngresoRequest {
  proveedorId?: string | null;
  documento?: string | null;
  observaciones?: string | null;
  items: IngresoItemRequest[];
}

export interface IngresoResponse {
  id: string;
  fecha: string;
  documento?: string | null;
  proveedorNombre?: string | null;
  observaciones?: string | null;
  total: number;
  usuarioNombre: string;
  items: LoteResponse[];
}

export interface SalidaRequest {
  productoId: string;
  cantidad: number;
  motivo: MotivoSalida;
  observaciones?: string | null;
}

export interface AjusteRequest {
  loteId: string;
  cantidad: number;
  observaciones?: string | null;
}

export interface MovimientoResponse {
  id: string;
  fecha: string;
  tipo: TipoMovimiento;
  motivo: string;
  productoId: string;
  productoNombre: string;
  loteId?: string | null;
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
  productoId?: string | null;
  tipo?: TipoMovimiento | null;
  desde?: string | null;
  hasta?: string | null;
}
