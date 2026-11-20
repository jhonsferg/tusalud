export type EstadoRequisicion =
  'BORRADOR' | 'ENVIADA' | 'EN_COTIZACION' | 'ATENDIDA' | 'RECHAZADA' | 'ANULADA';

export type PrioridadRequisicion = 'NORMAL' | 'URGENTE';
export type AreaSolicitante = 'ALMACEN' | 'FARMACIA';
export type EstadoCotizacion = 'VIGENTE' | 'SELECCIONADA' | 'DESCARTADA';

export type EstadoOrdenCompra =
  | 'BORRADOR'
  | 'PENDIENTE_APROBACION'
  | 'APROBADA'
  | 'PARCIAL'
  | 'RECIBIDA'
  | 'RECHAZADA'
  | 'CERRADA'
  | 'ANULADA';

export type NivelAprobacion = 'FINANZAS' | 'GERENCIA';
export type TipoComprobanteProveedor = 'FACTURA' | 'BOLETA' | 'RECIBO';
export type EstadoComprobanteProveedor = 'PENDIENTE' | 'PARCIAL' | 'PAGADO' | 'ANULADO';
export type MetodoPagoProveedor = 'TRANSFERENCIA' | 'EFECTIVO' | 'CHEQUE' | 'YAPE_PLIN';

export const ETIQUETA_ESTADO_REQUISICION: Readonly<Record<EstadoRequisicion, string>> = {
  BORRADOR: 'Borrador',
  ENVIADA: 'Enviada',
  EN_COTIZACION: 'En cotización',
  ATENDIDA: 'Atendida',
  RECHAZADA: 'Rechazada',
  ANULADA: 'Anulada',
};

export const SEVERIDAD_ESTADO_REQUISICION: Readonly<
  Record<EstadoRequisicion, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
> = {
  BORRADOR: 'secondary',
  ENVIADA: 'info',
  EN_COTIZACION: 'warn',
  ATENDIDA: 'success',
  RECHAZADA: 'danger',
  ANULADA: 'secondary',
};

export const ETIQUETA_ESTADO_ORDEN_COMPRA: Readonly<Record<EstadoOrdenCompra, string>> = {
  BORRADOR: 'Borrador',
  PENDIENTE_APROBACION: 'Por aprobar',
  APROBADA: 'Aprobada',
  PARCIAL: 'Recibida en parte',
  RECIBIDA: 'Recibida',
  RECHAZADA: 'Rechazada',
  CERRADA: 'Cerrada',
  ANULADA: 'Anulada',
};

export const SEVERIDAD_ESTADO_ORDEN_COMPRA: Readonly<
  Record<EstadoOrdenCompra, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
> = {
  BORRADOR: 'secondary',
  PENDIENTE_APROBACION: 'warn',
  APROBADA: 'info',
  PARCIAL: 'warn',
  RECIBIDA: 'success',
  RECHAZADA: 'danger',
  CERRADA: 'secondary',
  ANULADA: 'secondary',
};

export const ETIQUETA_ESTADO_COMPROBANTE_PROVEEDOR: Readonly<
  Record<EstadoComprobanteProveedor, string>
> = {
  PENDIENTE: 'Pendiente',
  PARCIAL: 'Pago parcial',
  PAGADO: 'Pagado',
  ANULADO: 'Anulado',
};

export const SEVERIDAD_ESTADO_COMPROBANTE_PROVEEDOR: Readonly<
  Record<EstadoComprobanteProveedor, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
> = {
  PENDIENTE: 'warn',
  PARCIAL: 'info',
  PAGADO: 'success',
  ANULADO: 'secondary',
};

export const ETIQUETA_METODO_PAGO_PROVEEDOR: Readonly<Record<MetodoPagoProveedor, string>> = {
  TRANSFERENCIA: 'Transferencia',
  EFECTIVO: 'Efectivo',
  CHEQUE: 'Cheque',
  YAPE_PLIN: 'Yape o Plin',
};

export const ETIQUETA_TIPO_COMPROBANTE_PROVEEDOR: Readonly<
  Record<TipoComprobanteProveedor, string>
> = {
  FACTURA: 'Factura',
  BOLETA: 'Boleta',
  RECIBO: 'Recibo',
};

export interface ConfiguracionCompras {
  limiteFinanzas: number;
  tasaIgv: number;
}

export interface RequisicionItemRequest {
  productoId: string;
  cantidad: number;
  observacion?: string | null;
}

export interface RequisicionRequest {
  prioridad: PrioridadRequisicion;
  motivo?: string | null;
  sugerida?: boolean;
  items: RequisicionItemRequest[];
}

export interface RequisicionItemResponse {
  id: string;
  productoId: string;
  productoCodigo: string;
  productoNombre: string;
  unidad: string;
  cantidad: number;
  observacion?: string | null;
  stockActual: number;
  stockMinimo: number;
}

export interface RequisicionResumen {
  id: string;
  codigo: string;
  estado: EstadoRequisicion;
  prioridad: PrioridadRequisicion;
  area: AreaSolicitante;
  solicitanteId: string;
  solicitanteNombre: string;
  motivo?: string | null;
  sugerida: boolean;
  cantidadItems: number;
  creadaEn: string;
  enviadaEn?: string | null;
}

export interface OrdenReferencia {
  id: string;
  codigo: string;
  estado: EstadoOrdenCompra;
  total: number;
}

export interface RequisicionDetalle {
  resumen: RequisicionResumen;
  items: RequisicionItemResponse[];
  motivoResolucion?: string | null;
  resueltaPorNombre?: string | null;
  resueltaEn?: string | null;
  cotizaciones: number;
  ordenes: OrdenReferencia[];
}

export interface SugerenciaRequisicion {
  productoId: string;
  productoCodigo: string;
  productoNombre: string;
  unidad: string;
  stockActual: number;
  stockMinimo: number;
  cantidadSugerida: number;
}

export interface CotizacionItemRequest {
  productoId: string;
  precioUnitario: number;
}

export interface CotizacionRequest {
  proveedorId: string;
  vigenciaHasta?: string | null;
  plazoEntregaDias?: number | null;
  condicionPagoDias?: number | null;
  observaciones?: string | null;
  items: CotizacionItemRequest[];
}

export interface CotizacionItemResponse {
  id: string;
  productoId: string;
  productoNombre: string;
  unidad: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface CotizacionResponse {
  id: string;
  requisicionId: string;
  proveedorId: string;
  proveedorNombre: string;
  proveedorRuc: string;
  estado: EstadoCotizacion;
  vigenciaHasta?: string | null;
  vencida: boolean;
  plazoEntregaDias?: number | null;
  condicionPagoDias: number;
  observaciones?: string | null;
  total: number;
  registradaPorNombre: string;
  creadaEn: string;
  items: CotizacionItemResponse[];
}

export interface ColumnaComparativa {
  cotizacionId: string;
  proveedorNombre: string;
  estado: EstadoCotizacion;
  total: number;
  cubreTodo: boolean;
  plazoEntregaDias?: number | null;
  condicionPagoDias: number;
  mejorTotal: boolean;
}

export interface FilaComparativa {
  productoId: string;
  productoNombre: string;
  unidad: string;
  cantidad: number;
  precios: (number | null)[];
  mejorCotizacionId?: string | null;
}

export interface ComparativaResponse {
  requisicionId: string;
  columnas: ColumnaComparativa[];
  filas: FilaComparativa[];
}

export interface OrdenItemRequest {
  productoId: string;
  cantidad: number;
  precioUnitario: number;
}

export interface OrdenCompraRequest {
  proveedorId: string;
  requisicionId?: string | null;
  cotizacionId?: string | null;
  afectaIgv: boolean;
  condicionPagoDias?: number | null;
  fechaEntregaEstimada?: string | null;
  observaciones?: string | null;
  items: OrdenItemRequest[];
}

export interface OrdenItemResponse {
  id: string;
  productoId: string;
  productoCodigo: string;
  productoNombre: string;
  unidad: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  recibida: number;
  rechazada: number;
  pendiente: number;
}

export interface OrdenCompraResumen {
  id: string;
  codigo?: string | null;
  estado: EstadoOrdenCompra;
  proveedorId: string;
  proveedorNombre: string;
  total: number;
  nivelAprobacion?: NivelAprobacion | null;
  creadaPorNombre: string;
  requisicionCodigo?: string | null;
  creadaEn: string;
  aprobadaEn?: string | null;
  porcentajeRecibido: number;
}

export interface RecepcionResumen {
  id: string;
  codigo: string;
  ordenId: string;
  ordenCodigo: string;
  proveedorNombre: string;
  guiaRemision?: string | null;
  recibidaPorNombre: string;
  recibidaEn: string;
  conDiscrepancia: boolean;
  ingresoId?: string | null;
}

export interface OrdenCompraDetalle {
  resumen: OrdenCompraResumen;
  proveedorId: string;
  proveedorRuc: string;
  requisicionId?: string | null;
  cotizacionId?: string | null;
  afectaIgv: boolean;
  subtotal: number;
  igv: number;
  total: number;
  condicionPagoDias: number;
  fechaEntregaEstimada?: string | null;
  observaciones?: string | null;
  hash?: string | null;
  aprobadaPorNombre?: string | null;
  motivoResolucion?: string | null;
  enviadaEn?: string | null;
  cerradaEn?: string | null;
  anuladaEn?: string | null;
  facturado: number;
  items: OrdenItemResponse[];
  recepciones: RecepcionResumen[];
}

export interface RecepcionItemRequest {
  ordenItemId: string;
  cantidadRecibida: number;
  cantidadRechazada?: number | null;
  numeroLote?: string | null;
  fechaVencimiento?: string | null;
  ubicacion?: string | null;
  motivoDiscrepancia?: string | null;
}

export interface RecepcionRequest {
  guiaRemision?: string | null;
  observaciones?: string | null;
  items: RecepcionItemRequest[];
}

export interface RecepcionItemResponse {
  id: string;
  ordenItemId: string;
  productoId: string;
  productoNombre: string;
  recibida: number;
  rechazada: number;
  devuelta: number;
  numeroLote?: string | null;
  fechaVencimiento?: string | null;
  ubicacion?: string | null;
  loteId?: string | null;
  costoUnitario: number;
  motivoDiscrepancia?: string | null;
}

export interface RecepcionDetalle {
  resumen: RecepcionResumen;
  observaciones?: string | null;
  items: RecepcionItemResponse[];
}

export interface DevolucionRequest {
  recepcionId: string;
  comprobanteId?: string | null;
  motivo: string;
  items: { recepcionItemId: string; cantidad: number }[];
}

export interface DevolucionItemResponse {
  id: string;
  productoNombre: string;
  numeroLote: string;
  cantidad: number;
  costoUnitario: number;
}

export interface DevolucionResponse {
  id: string;
  codigo: string;
  proveedorId: string;
  proveedorNombre: string;
  recepcionId: string;
  recepcionCodigo: string;
  comprobanteId?: string | null;
  motivo: string;
  monto: number;
  registradaPorNombre: string;
  registradaEn: string;
  items: DevolucionItemResponse[];
}

export interface ComprobanteProveedorRequest {
  proveedorId: string;
  ordenId?: string | null;
  tipo: TipoComprobanteProveedor;
  serie: string;
  numero: string;
  rucEmisor: string;
  fechaEmision: string;
  fechaVencimiento?: string | null;
  subtotal: number;
  igv?: number | null;
  observaciones?: string | null;
}

export interface PagoProveedorRequest {
  monto: number;
  metodo: MetodoPagoProveedor;
  referencia?: string | null;
  fecha?: string | null;
}

export interface PagoProveedorResponse {
  id: string;
  monto: number;
  metodo: MetodoPagoProveedor;
  referencia?: string | null;
  fecha: string;
  registradoPorNombre: string;
  registradoEn: string;
}

export interface ComprobanteProveedorResponse {
  id: string;
  proveedorId: string;
  proveedorNombre: string;
  proveedorRuc: string;
  ordenId?: string | null;
  ordenCodigo?: string | null;
  tipo: TipoComprobanteProveedor;
  numero: string;
  fechaEmision: string;
  fechaVencimiento: string;
  subtotal: number;
  igv: number;
  total: number;
  montoPagado: number;
  montoCredito: number;
  saldo: number;
  estado: EstadoComprobanteProveedor;
  vencido: boolean;
  diasVencido: number;
  observaciones?: string | null;
  registradoPorNombre: string;
  registradoEn: string;
  motivoAnulacion?: string | null;
  pagos: PagoProveedorResponse[];
}

export interface TramoVencimiento {
  etiqueta: string;
  monto: number;
  cantidad: number;
}

export interface SaldoProveedor {
  proveedorId: string;
  proveedorNombre: string;
  saldo: number;
  vencido: number;
  comprobantes: number;
}

export interface ResumenCuentasPorPagar {
  totalPendiente: number;
  totalVencido: number;
  cantidadPendientes: number;
  cantidadVencidas: number;
  tramos: TramoVencimiento[];
  porProveedor: SaldoProveedor[];
}

export interface ReporteCompraProveedor {
  proveedorId: string;
  proveedorNombre: string;
  proveedorRuc: string;
  ordenes: number;
  montoOrdenado: number;
  montoRecibido: number;
  facturado: number;
  pagado: number;
  saldo: number;
}

export interface ResumenCompras {
  requisicionesAbiertas: number;
  ordenesPorAprobar: number;
  ordenesEnTransito: number;
  porPagar?: number | null;
  vencido?: number | null;
  limiteFinanzas: number;
}

export interface ComprasAuditoria {
  id: string;
  fecha: string;
  usuarioNombre: string;
  entidad: string;
  entidadId: string;
  accion: string;
  detalle?: string | null;
}

export interface FiltroRequisiciones {
  estado?: EstadoRequisicion | null;
  desde?: string | null;
  hasta?: string | null;
}

export interface FiltroOrdenesCompra {
  estado?: EstadoOrdenCompra | null;
  proveedorId?: string | null;
  desde?: string | null;
  hasta?: string | null;
}

export interface FiltroComprobantes {
  estado?: EstadoComprobanteProveedor | null;
  proveedorId?: string | null;
  desde?: string | null;
  hasta?: string | null;
  soloVencidos?: boolean;
}
