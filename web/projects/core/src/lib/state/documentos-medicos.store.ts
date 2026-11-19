import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  ActualizarDocumentoRequest,
  ConsentimientoDetalle,
  ConsentimientoPlantilla,
  ConsentimientoPlantillaRequest,
  ConsentimientoResumen,
  ConsentimientoSolicitudRequest,
  DocumentoMedicoDetalle,
  DocumentoMedicoRequest,
  DocumentoMedicoResumen,
  EstablecimientoRequest,
  EstablecimientoSalud,
  FirmaConsentimientoRequest,
  ReferenciaDocumento,
  ReferenciaEstadoRequest,
} from '../models';
import { DocumentosMedicosApiService } from '../services/api/documentos-medicos-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class DocumentosMedicosStore {
  private readonly api = inject(DocumentosMedicosApiService);

  readonly documentos = new AsyncState<DocumentoMedicoResumen[]>();
  readonly detalle = new AsyncState<DocumentoMedicoDetalle>();
  readonly establecimientos = new AsyncState<EstablecimientoSalud[]>();
  readonly plantillas = new AsyncState<ConsentimientoPlantilla[]>();
  readonly consentimientos = new AsyncState<ConsentimientoResumen[]>();
  readonly consentimiento = new AsyncState<ConsentimientoDetalle>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarMios(): void {
    this.documentos.load(this.api.mios());
  }

  cargarEmitidos(): void {
    this.documentos.load(this.api.emitidos());
  }

  cargarDelPaciente(pacienteId: string): void {
    this.documentos.load(this.api.delPaciente(pacienteId));
  }

  cargarDetalle(id: string): void {
    this.detalle.reset();
    this.detalle.load(this.api.obtener(id));
  }

  crear(request: DocumentoMedicoRequest): Observable<DocumentoMedicoDetalle> {
    return this.api.crear(request).pipe(tap((detalle) => this.detalle.set(detalle)));
  }

  epicrisisBorrador(ingresoId: string): Observable<DocumentoMedicoDetalle> {
    return this.api.epicrisisBorrador(ingresoId).pipe(tap((detalle) => this.detalle.set(detalle)));
  }

  actualizar(id: string, request: ActualizarDocumentoRequest): Observable<DocumentoMedicoDetalle> {
    return this.api.actualizar(id, request).pipe(tap((detalle) => this.detalle.set(detalle)));
  }

  emitir(id: string): Observable<DocumentoMedicoDetalle> {
    return this.api.emitir(id).pipe(tap((detalle) => this.detalle.set(detalle)));
  }

  anular(id: string, motivo: string): Observable<DocumentoMedicoDetalle> {
    return this.api.anular(id, motivo).pipe(tap((detalle) => this.detalle.set(detalle)));
  }

  cambiarEstadoReferencia(
    id: string,
    request: ReferenciaEstadoRequest,
  ): Observable<ReferenciaDocumento> {
    return this.api.estadoReferencia(id, request).pipe(tap(() => this.cargarDetalle(id)));
  }

  cargarEstablecimientos(): void {
    this.establecimientos.load(this.api.establecimientos());
  }

  guardarEstablecimiento(
    id: string | null,
    request: EstablecimientoRequest,
  ): Observable<EstablecimientoSalud> {
    const operacion = id
      ? this.api.actualizarEstablecimiento(id, request)
      : this.api.crearEstablecimiento(request);
    return operacion.pipe(tap(() => this.cargarEstablecimientos()));
  }

  cargarPlantillas(): void {
    this.plantillas.load(this.api.plantillasConsentimiento());
  }

  guardarPlantilla(
    id: string | null,
    request: ConsentimientoPlantillaRequest,
  ): Observable<ConsentimientoPlantilla> {
    const operacion = id
      ? this.api.actualizarPlantillaConsentimiento(id, request)
      : this.api.crearPlantillaConsentimiento(request);
    return operacion.pipe(tap(() => this.cargarPlantillas()));
  }

  cargarConsentimientosMios(): void {
    this.consentimientos.load(this.api.consentimientosMios());
  }

  cargarConsentimientosDelPaciente(pacienteId: string): void {
    this.consentimientos.load(this.api.consentimientosDelPaciente(pacienteId));
  }

  solicitarConsentimiento(
    request: ConsentimientoSolicitudRequest,
  ): Observable<ConsentimientoResumen> {
    return this.api.solicitarConsentimiento(request);
  }

  firmarConsentimiento(
    id: string,
    request: FirmaConsentimientoRequest,
  ): Observable<ConsentimientoDetalle> {
    return this.api.firmarConsentimiento(id, request).pipe(tap((d) => this.consentimiento.set(d)));
  }

  revocarConsentimiento(id: string, motivo: string): Observable<ConsentimientoDetalle> {
    return this.api.revocarConsentimiento(id, motivo).pipe(tap((d) => this.consentimiento.set(d)));
  }

  cargarConsentimiento(id: string): void {
    this.consentimiento.reset();
    this.consentimiento.load(this.api.consentimiento(id));
  }

  reset(): void {
    this.documentos.reset();
    this.detalle.reset();
    this.establecimientos.reset();
    this.plantillas.reset();
    this.consentimientos.reset();
    this.consentimiento.reset();
  }
}
