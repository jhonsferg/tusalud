export type TipoCpe = 'FACTURA' | 'BOLETA' | 'NOTA_CREDITO' | 'NOTA_DEBITO';

export type EstadoCpe =
  'PENDIENTE' | 'ACEPTADO' | 'ACEPTADO_OBSERVACIONES' | 'RECHAZADO' | 'ANULADO' | 'ERROR';

export type AfectacionCpe = 'GRAVADO' | 'EXONERADO' | 'INAFECTO';

export type ProveedorFiscal = 'SIMULACION' | 'SUNAT' | 'OSE' | 'PSE';

export type ModoFiscal = 'BETA' | 'PRODUCCION';

export type TipoResumen = 'RESUMEN_DIARIO' | 'COMUNICACION_BAJA';

export type EstadoResumen = 'PENDIENTE' | 'ENVIADO' | 'ACEPTADO' | 'RECHAZADO' | 'ERROR';

export type OperacionSunat = 'ENVIO_COMPROBANTE' | 'ENVIO_RESUMEN' | 'CONSULTA_TICKET';

export const ETIQUETA_TIPO_CPE: Readonly<Record<TipoCpe, string>> = {
  FACTURA: 'Factura',
  BOLETA: 'Boleta',
  NOTA_CREDITO: 'Nota de crédito',
  NOTA_DEBITO: 'Nota de débito',
};

export const ETIQUETA_ESTADO_CPE: Readonly<Record<EstadoCpe, string>> = {
  PENDIENTE: 'Pendiente',
  ACEPTADO: 'Aceptado',
  ACEPTADO_OBSERVACIONES: 'Con observaciones',
  RECHAZADO: 'Rechazado',
  ANULADO: 'Anulado',
  ERROR: 'Error de envío',
};

export const SEVERIDAD_ESTADO_CPE: Readonly<
  Record<EstadoCpe, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
> = {
  PENDIENTE: 'info',
  ACEPTADO: 'success',
  ACEPTADO_OBSERVACIONES: 'warn',
  RECHAZADO: 'danger',
  ANULADO: 'secondary',
  ERROR: 'danger',
};

export const ETIQUETA_AFECTACION: Readonly<Record<AfectacionCpe, string>> = {
  GRAVADO: 'Gravado con IGV',
  EXONERADO: 'Exonerado',
  INAFECTO: 'Inafecto',
};

export const ETIQUETA_PROVEEDOR_FISCAL: Readonly<Record<ProveedorFiscal, string>> = {
  SIMULACION: 'Simulación para pruebas',
  SUNAT: 'SUNAT directo',
  OSE: 'Operador de servicios electrónicos',
  PSE: 'Proveedor de servicios electrónicos',
};

export const ETIQUETA_ESTADO_RESUMEN: Readonly<Record<EstadoResumen, string>> = {
  PENDIENTE: 'Pendiente',
  ENVIADO: 'Esperando constancia',
  ACEPTADO: 'Aceptado',
  RECHAZADO: 'Rechazado',
  ERROR: 'Error',
};

export const ETIQUETA_OPERACION_SUNAT: Readonly<Record<OperacionSunat, string>> = {
  ENVIO_COMPROBANTE: 'Envío del comprobante',
  ENVIO_RESUMEN: 'Envío del resumen',
  CONSULTA_TICKET: 'Consulta del ticket',
};

export interface ConfiguracionFiscal {
  id: string;
  sede: string;
  habilitada: boolean;
  ruc: string;
  razonSocial: string;
  nombreComercial: string | null;
  direccion: string;
  ubigeo: string;
  departamento: string;
  provincia: string;
  distrito: string;
  codigoLocal: string;
  proveedor: ProveedorFiscal;
  modo: ModoFiscal;
  urlServicio: string | null;
  afectacionServicios: AfectacionCpe;
  igvPorcentaje: number;
  boletasPorResumen: boolean;
  diasLimiteBaja: number;
  certificadoDePrueba: boolean;
  credencialesSolConfiguradas: boolean;
  actualizadoEn: string;
}

export interface ConfiguracionFiscalRequest {
  habilitada: boolean;
  ruc: string;
  razonSocial: string;
  nombreComercial: string | null;
  direccion: string;
  ubigeo: string;
  departamento: string;
  provincia: string;
  distrito: string;
  codigoLocal: string;
  proveedor: ProveedorFiscal;
  modo: ModoFiscal;
  urlServicio: string | null;
  afectacionServicios: AfectacionCpe;
  boletasPorResumen: boolean;
  diasLimiteBaja: number;
}

export interface SerieComprobante {
  id: string;
  sede: string;
  tipo: TipoCpe;
  serie: string;
  correlativoActual: number;
  activa: boolean;
}

export interface SerieRequest {
  tipo: TipoCpe;
  serie: string;
  correlativoInicial: number | null;
}

export interface ItemCpe {
  posicion: number;
  codigo: string | null;
  descripcion: string;
  unidad: string;
  cantidad: number;
  precioUnitario: number;
  valorVenta: number;
  afectacion: AfectacionCpe;
  igv: number;
  total: number;
}

export interface IntercambioSunat {
  id: string;
  operacion: OperacionSunat;
  proveedor: ProveedorFiscal;
  exitoso: boolean;
  codigoRespuesta: string | null;
  mensaje: string | null;
  duracionMs: number;
  creadoEn: string;
}

export interface DocumentoElectronicoResumen {
  id: string;
  tipo: TipoCpe;
  serie: string;
  numero: number;
  numeroCompleto: string;
  emitidoEn: string;
  clienteTipoDoc: 'DNI' | 'RUC' | 'SIN_DOC';
  clienteDoc: string | null;
  clienteNombre: string;
  total: number;
  estado: EstadoCpe;
  cdrDescripcion: string | null;
  intentos: number;
  comprobanteId: string | null;
  documentoReferenciaId: string | null;
}

export interface DocumentoElectronicoDetalle {
  id: string;
  tipo: TipoCpe;
  serie: string;
  numero: number;
  numeroCompleto: string;
  emitidoEn: string;
  moneda: string;
  clienteTipoDoc: 'DNI' | 'RUC' | 'SIN_DOC';
  clienteDoc: string | null;
  clienteNombre: string;
  clienteDireccion: string | null;
  opGravada: number;
  opExonerada: number;
  opInafecta: number;
  igv: number;
  total: number;
  estado: EstadoCpe;
  motivoCodigo: string | null;
  motivoDescripcion: string | null;
  digestValue: string | null;
  xmlSha256: string | null;
  cdrCodigo: string | null;
  cdrDescripcion: string | null;
  cdrRecibidoEn: string | null;
  intentos: number;
  proximoIntento: string | null;
  ultimoError: string | null;
  motivoBaja: string | null;
  comprobanteId: string | null;
  documentoReferenciaId: string | null;
  documentoReferenciaNumero: string | null;
  acreditado: number;
  items: ItemCpe[];
  intercambios: IntercambioSunat[];
}

export interface ResumenSunat {
  id: string;
  tipo: TipoResumen;
  identificador: string;
  fechaReferencia: string;
  fechaGeneracion: string;
  estado: EstadoResumen;
  ticket: string | null;
  cdrCodigo: string | null;
  cdrDescripcion: string | null;
  motivo: string | null;
  intentos: number;
  ultimoError: string | null;
  documentos: number;
  creadoEn: string;
}

export interface FilaRegistroVentas {
  fecha: string;
  tipo: string;
  serie: string;
  numero: number;
  clienteTipoDoc: string;
  clienteDoc: string | null;
  clienteNombre: string;
  gravada: number;
  exonerada: number;
  inafecta: number;
  igv: number;
  total: number;
  estado: EstadoCpe;
}

export interface RegistroVentas {
  desde: string;
  hasta: string;
  filas: FilaRegistroVentas[];
  totalGravada: number;
  totalExonerada: number;
  totalInafecta: number;
  totalIgv: number;
  total: number;
}

export interface EstadoFacturacion {
  habilitada: boolean;
  proveedor: ProveedorFiscal | null;
  modo: ModoFiscal | null;
  certificadoDePrueba: boolean;
  conteo: Record<EstadoCpe, number>;
  pendientesDeEnvio: number;
  ultimoIntercambio: string | null;
}

export interface MotivoNota {
  codigo: string;
  descripcion: string;
  total: boolean;
  tipo: TipoCpe;
}

export interface FiltroDocumentos {
  desde?: string;
  hasta?: string;
  tipo?: TipoCpe | null;
  estado?: EstadoCpe | null;
  q?: string;
}

export interface NotaItemRequest {
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  afectacion?: AfectacionCpe | null;
}

export interface NotaRequest {
  documentoId: string;
  tipo: 'NOTA_CREDITO' | 'NOTA_DEBITO';
  motivoCodigo: string;
  descripcion: string;
  items: NotaItemRequest[] | null;
}

export interface CorreccionClienteRequest {
  tipoDoc: 'DNI' | 'RUC' | 'SIN_DOC';
  documento: string | null;
  nombre: string;
  direccion: string | null;
}
