export type TipoCuentaContable = 'ACTIVO' | 'PASIVO' | 'PATRIMONIO' | 'INGRESO' | 'GASTO';
export type NaturalezaCuenta = 'DEUDORA' | 'ACREEDORA';
export type EstadoPeriodo = 'ABIERTO' | 'CERRADO';
export type EstadoAsiento = 'BORRADOR' | 'CONTABILIZADO' | 'REVERTIDO';
export type OrigenAsiento =
  'MANUAL' | 'COBRO' | 'COMPRA_PROVEEDOR' | 'PAGO_PROVEEDOR' | 'MOVIMIENTO_TESORERIA' | 'REVERSA';
export type EventoContable =
  | 'COBRO_EFECTIVO'
  | 'COBRO_ELECTRONICO'
  | 'COMPRA_PROVEEDOR'
  | 'PAGO_PROVEEDOR_EFECTIVO'
  | 'PAGO_PROVEEDOR_BANCO';
export type LadoAsiento = 'DEBE' | 'HABER';
export type ConceptoMonto = 'TOTAL' | 'SUBTOTAL' | 'IGV' | 'CUBIERTO' | 'INGRESO_BRUTO' | 'MONTO';
export type TipoCuentaTesoreria = 'CAJA' | 'BANCO';
export type TipoMovimientoTesoreria = 'INGRESO' | 'EGRESO';
export type OrigenMovimiento = 'MANUAL' | 'COBRO' | 'PAGO_PROVEEDOR';
export type EstadoLineaExtracto = 'PENDIENTE' | 'CONCILIADA' | 'IGNORADA';
export type LibroElectronicoTipo = 'DIARIO' | 'MAYOR' | 'COMPRAS';

export const ETIQUETA_TIPO_CUENTA_CONTABLE: Readonly<Record<TipoCuentaContable, string>> = {
  ACTIVO: 'Activo',
  PASIVO: 'Pasivo',
  PATRIMONIO: 'Patrimonio',
  INGRESO: 'Ingreso',
  GASTO: 'Gasto',
};

export const ETIQUETA_ESTADO_ASIENTO: Readonly<Record<EstadoAsiento, string>> = {
  BORRADOR: 'Borrador',
  CONTABILIZADO: 'Contabilizado',
  REVERTIDO: 'Revertido',
};

export const SEVERIDAD_ESTADO_ASIENTO: Readonly<
  Record<EstadoAsiento, 'secondary' | 'success' | 'warn'>
> = {
  BORRADOR: 'secondary',
  CONTABILIZADO: 'success',
  REVERTIDO: 'warn',
};

export const ETIQUETA_ORIGEN_ASIENTO: Readonly<Record<OrigenAsiento, string>> = {
  MANUAL: 'Manual',
  COBRO: 'Cobro en caja',
  COMPRA_PROVEEDOR: 'Compra a proveedor',
  PAGO_PROVEEDOR: 'Pago a proveedor',
  MOVIMIENTO_TESORERIA: 'Tesorería',
  REVERSA: 'Reversa',
};

export const ETIQUETA_EVENTO_CONTABLE: Readonly<Record<EventoContable, string>> = {
  COBRO_EFECTIVO: 'Cobro en efectivo',
  COBRO_ELECTRONICO: 'Cobro electrónico',
  COMPRA_PROVEEDOR: 'Comprobante de proveedor',
  PAGO_PROVEEDOR_EFECTIVO: 'Pago a proveedor en efectivo',
  PAGO_PROVEEDOR_BANCO: 'Pago a proveedor por banco',
};

export const ETIQUETA_CONCEPTO_MONTO: Readonly<Record<ConceptoMonto, string>> = {
  TOTAL: 'Total del comprobante',
  SUBTOTAL: 'Base imponible',
  IGV: 'IGV',
  CUBIERTO: 'Cubierto por la aseguradora',
  INGRESO_BRUTO: 'Ingreso bruto',
  MONTO: 'Monto del pago',
};

export const ETIQUETA_TIPO_MOVIMIENTO: Readonly<Record<TipoMovimientoTesoreria, string>> = {
  INGRESO: 'Ingreso',
  EGRESO: 'Egreso',
};

export const ETIQUETA_ORIGEN_MOVIMIENTO: Readonly<Record<OrigenMovimiento, string>> = {
  MANUAL: 'Manual',
  COBRO: 'Cobro',
  PAGO_PROVEEDOR: 'Pago a proveedor',
};

export const ETIQUETA_ESTADO_LINEA_EXTRACTO: Readonly<Record<EstadoLineaExtracto, string>> = {
  PENDIENTE: 'Pendiente',
  CONCILIADA: 'Conciliada',
  IGNORADA: 'Ignorada',
};

export const ETIQUETA_LIBRO_ELECTRONICO: Readonly<Record<LibroElectronicoTipo, string>> = {
  DIARIO: 'Libro diario',
  MAYOR: 'Libro mayor',
  COMPRAS: 'Registro de compras',
};

export interface CuentaContableResponse {
  id: string;
  codigo: string;
  nombre: string;
  tipo: TipoCuentaContable;
  naturaleza: NaturalezaCuenta;
  aceptaMovimientos: boolean;
  activa: boolean;
  sistema: boolean;
}

export interface CuentaContableRequest {
  codigo: string;
  nombre: string;
  tipo: TipoCuentaContable;
  aceptaMovimientos: boolean;
}

export interface ActualizarCuentaContableRequest {
  nombre: string;
  aceptaMovimientos: boolean;
  activa: boolean;
}

export interface PeriodoResponse {
  id: string;
  anio: number;
  mes: number;
  etiqueta: string;
  estado: EstadoPeriodo;
  inicio: string;
  fin: string;
  asientos: number;
  borradores: number;
  cerradoEn: string | null;
  cerradoPor: string | null;
  reabiertoEn: string | null;
  reabiertoPor: string | null;
  motivoReapertura: string | null;
}

export interface LineaAsientoResponse {
  id: string;
  posicion: number;
  cuentaId: string;
  cuentaCodigo: string;
  cuentaNombre: string;
  debe: number;
  haber: number;
  glosa: string | null;
}

export interface AsientoResumen {
  id: string;
  numero: string | null;
  fecha: string;
  periodo: string;
  glosa: string;
  origen: OrigenAsiento;
  estado: EstadoAsiento;
  totalDebe: number;
  totalHaber: number;
}

export interface AsientoResponse {
  id: string;
  numero: string | null;
  fecha: string;
  periodoId: string;
  periodo: string;
  glosa: string;
  origen: OrigenAsiento;
  referenciaId: string | null;
  estado: EstadoAsiento;
  totalDebe: number;
  totalHaber: number;
  creadoPor: string;
  creadoEn: string;
  contabilizadoPor: string;
  contabilizadoEn: string | null;
  revierteAId: string | null;
  revierteANumero: string | null;
  reversaId: string | null;
  motivoReversa: string | null;
  lineas: LineaAsientoResponse[];
}

export interface LineaAsientoRequest {
  cuentaId: string;
  debe: number | null;
  haber: number | null;
  glosa: string | null;
}

export interface AsientoRequest {
  fecha: string;
  glosa: string;
  lineas: LineaAsientoRequest[];
}

export interface FiltroAsientos {
  periodoId?: string | null;
  estado?: EstadoAsiento | null;
  origen?: OrigenAsiento | null;
  desde?: string | null;
  hasta?: string | null;
  q?: string | null;
}

export interface PlantillaLineaResponse {
  id: string;
  posicion: number;
  lado: LadoAsiento;
  concepto: ConceptoMonto;
  cuentaId: string;
  cuentaCodigo: string;
  cuentaNombre: string;
}

export interface PlantillaAsientoResponse {
  id: string;
  evento: EventoContable;
  nombre: string;
  activa: boolean;
  lineas: PlantillaLineaResponse[];
}

export interface PlantillaAsientoRequest {
  nombre: string;
  activa: boolean;
  cuentas: { lineaId: string; cuentaId: string }[];
}

export interface AsientoPendienteResponse {
  id: string;
  evento: string;
  referenciaId: string;
  error: string;
  intentos: number;
  creadoEn: string;
  resueltoEn: string | null;
}

export interface CuentaTesoreriaResponse {
  id: string;
  nombre: string;
  tipo: TipoCuentaTesoreria;
  banco: string | null;
  ultimos4: string | null;
  moneda: string;
  cuentaContableId: string;
  cuentaContableCodigo: string;
  saldoInicial: number;
  saldo: number;
  predeterminada: boolean;
  activa: boolean;
}

export interface CuentaTesoreriaRequest {
  nombre: string;
  tipo: TipoCuentaTesoreria;
  banco: string | null;
  ultimos4: string | null;
  cuentaContableId: string;
  saldoInicial: number;
  predeterminada: boolean;
  activa?: boolean | null;
}

export interface MovimientoTesoreriaResponse {
  id: string;
  cuentaId: string;
  cuentaNombre: string;
  fecha: string;
  tipo: TipoMovimientoTesoreria;
  monto: number;
  descripcion: string;
  referencia: string | null;
  origen: OrigenMovimiento;
  asientoId: string | null;
  conciliado: boolean;
}

export interface MovimientoTesoreriaRequest {
  cuentaId: string;
  fecha: string;
  tipo: TipoMovimientoTesoreria;
  monto: number;
  descripcion: string;
  referencia: string | null;
  contrapartidaId: string;
}

export interface FiltroMovimientosTesoreria {
  cuentaId?: string | null;
  desde?: string | null;
  hasta?: string | null;
  conciliado?: boolean | null;
}

export interface ExtractoResponse {
  id: string;
  cuentaId: string;
  cuentaNombre: string;
  archivo: string;
  desde: string | null;
  hasta: string | null;
  lineas: number;
  duplicadas: number;
  importadoPor: string;
  importadoEn: string;
}

export interface ExtractoLineaResponse {
  id: string;
  fecha: string;
  descripcion: string;
  referencia: string | null;
  monto: number;
  estado: EstadoLineaExtracto;
  movimientoId: string | null;
}

export interface SugerenciaConciliacion {
  linea: ExtractoLineaResponse;
  movimiento: MovimientoTesoreriaResponse;
  puntaje: number;
}

export interface ResumenConciliacion {
  cuentaId: string;
  saldoLibros: number;
  lineasPendientes: number;
  netoExtractoPendiente: number;
  movimientosSinConciliar: number;
  netoLibrosPendiente: number;
  diferencia: number;
}

export interface FilaMayor {
  fecha: string;
  asiento: string;
  asientoId: string;
  glosa: string;
  debe: number;
  haber: number;
  saldo: number;
}

export interface LibroMayorResponse {
  cuenta: CuentaContableResponse;
  desde: string;
  hasta: string;
  saldoInicial: number;
  totalDebe: number;
  totalHaber: number;
  saldoFinal: number;
  movimientos: FilaMayor[];
}

export interface FilaBalance {
  codigo: string;
  nombre: string;
  tipo: TipoCuentaContable;
  agregada: boolean;
  inicialDeudor: number;
  inicialAcreedor: number;
  debe: number;
  haber: number;
  finalDeudor: number;
  finalAcreedor: number;
}

export interface BalanceComprobacionResponse {
  desde: string;
  hasta: string;
  filas: FilaBalance[];
  totalDebe: number;
  totalHaber: number;
  totalDeudor: number;
  totalAcreedor: number;
  cuadra: boolean;
}

export interface LineaEstado {
  codigo: string;
  nombre: string;
  importe: number;
}

export interface SeccionEstado {
  titulo: string;
  lineas: LineaEstado[];
  total: number;
}

export interface EstadoSituacionResponse {
  al: string;
  activo: SeccionEstado;
  pasivo: SeccionEstado;
  patrimonio: SeccionEstado;
  totalPasivoPatrimonio: number;
  cuadra: boolean;
}

export interface EstadoResultadosResponse {
  desde: string;
  hasta: string;
  ingresos: SeccionEstado;
  gastos: SeccionEstado;
  resultado: number;
}

export interface SemanaFlujo {
  desde: string;
  hasta: string;
  cobros: number;
  pagos: number;
  neto: number;
  saldoProyectado: number;
}

export interface FlujoCajaResponse {
  desde: string;
  semanas: number;
  saldoInicial: number;
  detalle: SemanaFlujo[];
  saldoFinal: number;
}

export interface ResumenContabilidad {
  periodoActual: string | null;
  estadoPeriodoActual: EstadoPeriodo | null;
  asientosDelPeriodo: number;
  borradores: number;
  pendientesDeContabilizar: number;
  saldoTesoreria: number;
  extractosSinConciliar: number;
  porPagar: number;
  porCobrar: number;
}

export interface ContabilidadAuditoria {
  id: string;
  fecha: string;
  usuario: string;
  entidad: string;
  entidadId: string;
  accion: string;
  detalle: string | null;
}

export interface LibroElectronico {
  nombreArchivo: string;
  contenido: string;
  registros: number;
  estado: string;
}
