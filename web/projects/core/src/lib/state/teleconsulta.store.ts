import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { ConsentimientoTelemedicinaResponse, TeleconsultaResponse } from '../models';
import { TeleconsultaApiService } from '../services/api/teleconsulta-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class TeleconsultaStore {
  private readonly api = inject(TeleconsultaApiService);

  readonly lista = new AsyncState<TeleconsultaResponse[]>();
  readonly detalle = new AsyncState<TeleconsultaResponse>();
  readonly consentimiento = new AsyncState<ConsentimientoTelemedicinaResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarMias(): void {
    this.lista.load(this.api.mias());
  }

  cargarDelMedico(desde?: string | null): void {
    this.lista.load(this.api.delMedico(desde));
  }

  cargar(id: string): void {
    this.detalle.load(this.api.obtener(id));
  }

  cargarConsentimiento(): void {
    this.consentimiento.load(this.api.consentimiento());
  }

  aceptarConsentimiento(version: string): Observable<ConsentimientoTelemedicinaResponse> {
    return this.api.aceptarConsentimiento(version).pipe(
      tap((respuesta) => {
        this.consentimiento.set(respuesta);
        this.detalle.update((actual) =>
          actual ? { ...actual, consentimientoPendiente: false } : actual,
        );
      }),
    );
  }

  abrir(id: string): Observable<TeleconsultaResponse> {
    return this.api.abrir(id).pipe(tap((t) => this.detalle.set(t)));
  }

  finalizar(id: string): Observable<TeleconsultaResponse> {
    return this.api.finalizar(id).pipe(tap((t) => this.detalle.set(t)));
  }

  reset(): void {
    this.lista.reset();
    this.detalle.reset();
    this.consentimiento.reset();
  }
}
