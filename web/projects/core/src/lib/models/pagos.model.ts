export type TipoCupon = 'PORCENTAJE' | 'MONTO_FIJO';
export type TipoMetodoPago = 'TARJETA_CREDITO' | 'TARJETA_DEBITO';
export type EstadoPago = 'PAGADO' | 'REEMBOLSADO';

export interface CuponResponse {
  id: string;
  codigo: string;
  descripcion?: string | null;
  tipo: TipoCupon;
  valor: number;
  fechaInicio: string;
  fechaFin: string;
  usosMaximos?: number | null;
  usosActuales: number;
  activo: boolean;
  vigente: boolean;
}

export interface CuponRequest {
  codigo: string;
  descripcion?: string | null;
  tipo: TipoCupon;
  valor: number;
  fechaInicio: string;
  fechaFin: string;
  usosMaximos?: number | null;
  activo: boolean;
}

export interface CuponValidacionResponse {
  codigo: string;
  valido: boolean;
  mensaje?: string | null;
  descuento: number;
  total: number;
}

export interface MetodoPagoResponse {
  id: string;
  tipo: TipoMetodoPago;
  titular: string;
  marca: string;
  ultimos4: string;
  vencimientoMes: number;
  vencimientoAnio: number;
  predeterminado: boolean;
  vencida: boolean;
}

export interface MetodoPagoRequest {
  tipo: TipoMetodoPago;
  titular: string;
  numeroTarjeta: string;
  vencimientoMes: number;
  vencimientoAnio: number;
  cvv: string;
  predeterminado: boolean;
}

export interface PagoRequest {
  metodoPagoId: string;
  codigoCupon?: string | null;
}

export interface PagoResponse {
  id: string;
  citaId: string;
  pacienteNombre: string;
  medicoNombre: string;
  especialidad: string;
  fechaCita: string;
  monto: number;
  descuento: number;
  total: number;
  codigoCupon?: string | null;
  estado: EstadoPago;
  comprobante: string;
  metodo: string;
  fechaPago: string;
}

export type Billetera = 'YAPE' | 'PLIN';
export type EstadoPagoQr = 'PENDIENTE' | 'CONFIRMADO' | 'EXPIRADO' | 'CANCELADO';

export interface PagoQrRequest {
  citaId: string;
  billetera: Billetera;
  codigoCupon?: string | null;
}

export interface PagoQrResponse {
  codigo: string;
  billetera: Billetera;
  monto: number;
  descuento: number;
  total: number;
  codigoCupon?: string | null;
  qrPayload: string;
  estado: EstadoPagoQr;
  expiraEn: string;
  pagoId?: string | null;
}
