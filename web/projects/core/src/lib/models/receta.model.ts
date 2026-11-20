import { MetodoPagoFarmacia } from './farmacia.model';

export type CategoriaControl = 'NINGUNA' | 'ANTIBIOTICO' | 'PSICOTROPICO' | 'ESTUPEFACIENTE';
export type EstadoVenta = 'VIGENTE' | 'SUSPENDIDO' | 'RETIRADO';
export type SeveridadInteraccion = 'LEVE' | 'MODERADA' | 'GRAVE' | 'CONTRAINDICADA';
export type CategoriaEmbarazo = 'A' | 'B' | 'C' | 'D' | 'X' | 'SIN_CLASIFICAR';
export type CompatibilidadLactancia =
  'COMPATIBLE' | 'PRECAUCION' | 'CONTRAINDICADO' | 'SIN_CLASIFICAR';
export type EstadoRecetaElectronica = 'EMITIDA' | 'DISPENSADA_PARCIAL' | 'DISPENSADA' | 'ANULADA';
export type TipoAlertaReceta =
  'ALERGIA' | 'INTERACCION' | 'DUPLICIDAD' | 'DOSIS_MAXIMA' | 'EMBARAZO' | 'LACTANCIA' | 'RENAL';
export type SeveridadAlerta = 'INFORMATIVA' | 'MODERADA' | 'GRAVE' | 'CONTRAINDICADA';

export interface PrincipioActivo {
  id: string;
  dci: string;
  claseTerapeutica: string | null;
  grupoAlergia: string | null;
  aliasAlergia: string | null;
  dosisMaxAdultoMgDia: number | null;
  dosisMaxPediatricaMgKgDia: number | null;
  categoriaEmbarazo: CategoriaEmbarazo;
  lactancia: CompatibilidadLactancia;
  avisoRenal: string | null;
  activo: boolean;
}

export interface PrincipioActivoRequest {
  dci: string;
  claseTerapeutica?: string | null;
  grupoAlergia?: string | null;
  aliasAlergia?: string | null;
  dosisMaxAdultoMgDia?: number | null;
  dosisMaxPediatricaMgKgDia?: number | null;
  categoriaEmbarazo?: CategoriaEmbarazo | null;
  lactancia?: CompatibilidadLactancia | null;
  avisoRenal?: string | null;
}

export interface Medicamento {
  id: string;
  principioActivoId: string;
  dci: string;
  claseTerapeutica: string | null;
  nombreComercial: string | null;
  formaFarmaceutica: string;
  concentracion: string;
  viaAdministracion: string;
  presentacion: string | null;
  categoriaControl: CategoriaControl;
  requiereReceta: boolean;
  estadoVenta: EstadoVenta;
  productoId: string | null;
  enPetitorio: boolean;
  activo: boolean;
  nombre: string;
  porValidar: boolean;
}

export interface MedicamentoRequest {
  principioActivoId: string;
  nombreComercial?: string | null;
  formaFarmaceutica: string;
  concentracion: string;
  viaAdministracion: string;
  presentacion?: string | null;
  categoriaControl?: CategoriaControl | null;
  requiereReceta?: boolean | null;
  estadoVenta?: EstadoVenta | null;
  productoId?: string | null;
  enPetitorio?: boolean | null;
  activo?: boolean | null;
}

export interface InteraccionMedicamentosa {
  id: string;
  principioAId: string;
  principioA: string;
  principioBId: string;
  principioB: string;
  severidad: SeveridadInteraccion;
  descripcion: string;
  recomendacion: string | null;
}

export interface InteraccionRequest {
  principioAId: string;
  principioBId: string;
  severidad: SeveridadInteraccion;
  descripcion: string;
  recomendacion?: string | null;
}

export interface ImportacionCatalogo {
  creados: number;
  actualizados: number;
  errores: string[];
}

export interface AlertaReceta {
  clave: string;
  tipo: TipoAlertaReceta;
  severidad: SeveridadAlerta;
  mensaje: string;
  recomendacion: string | null;
  medicamento: string;
  requiereJustificacion: boolean;
  minimoJustificacion: number;
}

export interface AlertasReceta {
  alertas: AlertaReceta[];
  pendientesDeJustificar: number;
  hayContraindicadas: boolean;
  porValidar: boolean;
}

export interface ItemPrescripcion {
  medicamentoId: string;
  dosisUnitariaMg?: number | null;
  vecesPorDia?: number | null;
}

export interface AlertasRecetaRequest {
  pacienteId: string;
  items: ItemPrescripcion[];
  embarazada?: boolean;
  lactando?: boolean;
}

export interface ItemEmisionReceta {
  atencionPosicion: number;
  medicamentoId: string;
  cantidad: number;
  via?: string | null;
  dosisUnitariaMg?: number | null;
  vecesPorDia?: number | null;
}

export interface JustificacionAlerta {
  clave: string;
  texto: string;
}

export interface EmitirRecetaRequest {
  atencionId: string;
  vigenciaDias?: number | null;
  indicaciones?: string | null;
  embarazada?: boolean;
  lactando?: boolean;
  reemplazaId?: string | null;
  items: ItemEmisionReceta[];
  justificaciones?: JustificacionAlerta[];
}

export interface ItemRecetaElectronica {
  id: string;
  posicion: number;
  atencionPosicion: number | null;
  medicamentoId: string | null;
  medicamento: string;
  dosis: string | null;
  frecuencia: string | null;
  duracion: string | null;
  via: string | null;
  dosisUnitariaMg: number | null;
  vecesPorDia: number | null;
  cantidad: number;
  cantidadDispensada: number;
  pendiente: number;
  controlEspecial: boolean;
  productoId: string | null;
}

export interface AlertaRegistrada {
  clave: string;
  tipo: TipoAlertaReceta;
  severidad: SeveridadAlerta;
  mensaje: string;
  justificacion: string | null;
  justificadaPor: string | null;
  justificadaEn: string | null;
}

export interface RecetaElectronica {
  id: string;
  numero: string;
  estado: EstadoRecetaElectronica;
  vigente: boolean;
  emitidaEn: string;
  vigenteHasta: string;
  atencionId: string;
  citaId: string;
  pacienteId: string;
  pacienteNombre: string;
  pacienteDni: string;
  medicoNombre: string;
  medicoColegiatura: string;
  diagnosticoCodigo: string | null;
  diagnosticoDescripcion: string | null;
  indicaciones: string | null;
  controlEspecial: boolean;
  codigoVerificacion: string;
  hash: string;
  firmaAlgoritmo: string;
  certificadoHuella: string;
  certificadoSujeto: string;
  certificadoDePrueba: boolean;
  firmaValida: boolean;
  anuladaEn: string | null;
  motivoAnulacion: string | null;
  reemplazaNumero: string | null;
  items: ItemRecetaElectronica[];
  alertas: AlertaRegistrada[];
}

export interface RecetaResumen {
  id: string;
  numero: string;
  estado: EstadoRecetaElectronica;
  vigente: boolean;
  emitidaEn: string;
  vigenteHasta: string;
  pacienteId: string;
  pacienteNombre: string;
  pacienteDni: string;
  medicoNombre: string;
  medicamentos: number;
  controlEspecial: boolean;
}

export interface RecetaFarmacia {
  id: string;
  numero: string;
  estado: EstadoRecetaElectronica;
  emitidaEn: string;
  vigenteHasta: string;
  atencionId: string;
  pacienteNombre: string;
  pacienteDni: string;
  medicoNombre: string;
  medicoColegiatura: string;
  controlEspecial: boolean;
  items: ItemRecetaElectronica[];
}

export interface ItemDispensarReceta {
  itemId: string;
  cantidad: number;
}

export interface DispensarRecetaRequest {
  items: ItemDispensarReceta[];
  metodoPago: MetodoPagoFarmacia;
  observaciones?: string | null;
}
