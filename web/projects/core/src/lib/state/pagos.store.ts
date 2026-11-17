import { Injectable, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { MetodoPagoRequest, MetodoPagoResponse, PagoRequest, PagoResponse } from '../models';
import { PagosApiService } from '../services/api/pagos-api.service';
import { AsyncState } from './async-state';
import { CitasStore } from './citas.store';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class PagosStore {
  private readonly api = inject(PagosApiService);
  private readonly citas = inject(CitasStore);

  readonly metodos = new AsyncState<MetodoPagoResponse[]>();
  readonly pagos = new AsyncState<PagoResponse[]>();

  readonly metodosVigentes = computed(() => (this.metodos.value() ?? []).filter((m) => !m.vencida));
  readonly predeterminado = computed(
    () => this.metodosVigentes().find((m) => m.predeterminado) ?? this.metodosVigentes()[0],
  );
  readonly totalPagado = computed(() =>
    (this.pagos.value() ?? [])
      .filter((pago) => pago.estado === 'PAGADO')
      .reduce((total, pago) => total + pago.total, 0),
  );

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarMetodos(): void {
    this.metodos.load(this.api.listarMetodos());
  }

  crearMetodo(request: MetodoPagoRequest): Observable<MetodoPagoResponse> {
    return this.api.crearMetodo(request).pipe(tap(() => this.cargarMetodos()));
  }

  marcarPredeterminado(id: string): Observable<MetodoPagoResponse> {
    return this.api
      .marcarPredeterminado(id)
      .pipe(
        tap(() =>
          this.metodos.update((items) =>
            items?.map((item) => ({ ...item, predeterminado: item.id === id })),
          ),
        ),
      );
  }

  eliminarMetodo(id: string): Observable<void> {
    return this.api.eliminarMetodo(id).pipe(tap(() => this.cargarMetodos()));
  }

  cargarPagos(): void {
    this.pagos.load(this.api.listarPagos());
  }

  pagarCita(citaId: string, request: PagoRequest): Observable<PagoResponse> {
    return this.api.pagarCita(citaId, request).pipe(
      tap((pago) => {
        this.pagos.update((items) => (items ? [pago, ...items] : items));
        this.citas.marcarPagada(citaId);
      }),
    );
  }

  registrarPagoConfirmado(pagoId: string, citaId: string): Observable<PagoResponse> {
    return this.api.obtenerPago(pagoId).pipe(
      tap((pago) => {
        this.pagos.update((items) => (items ? [pago, ...items] : items));
        this.citas.marcarPagada(citaId);
      }),
    );
  }

  reset(): void {
    this.metodos.reset();
    this.pagos.reset();
  }
}
