export type TipoCupon = 'PORCENTAJE' | 'MONTO_FIJO';
export type TipoMetodoPago = 'TARJETA_CREDITO' | 'TARJETA_DEBITO';
export type EstadoPago = 'PAGADO' | 'REEMBOLSADO';

export interface CuponResponse {
  id: number;
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
  id: number;
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
  metodoPagoId: number;
  codigoCupon?: string | null;
}

export interface PagoResponse {
  id: number;
  citaId: number;
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
