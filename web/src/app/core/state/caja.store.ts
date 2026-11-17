import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CierreCajaRequest,
  CobroRequest,
  ComprobanteResponse,
  PendienteCobroResponse,
  ReporteIngresosResponse,
  ResumenCajaResponse,
  SesionCajaResponse,
} from '@tusalud/app/core/models';
import { CajaApiService } from '@tusalud/app/core/services/api/caja-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class CajaStore {
  private readonly api = inject(CajaApiService);

  readonly sesion = new AsyncState<SesionCajaResponse>();
  readonly resumen = new AsyncState<ResumenCajaResponse>();
  readonly historial = new AsyncState<SesionCajaResponse[]>();
  readonly pendientes = new AsyncState<PendienteCobroResponse[]>();
  readonly comprobantes = new AsyncState<ComprobanteResponse[]>();
  readonly reporte = new AsyncState<ReporteIngresosResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarSesion(): void {
    this.sesion.reset();
    this.sesion.load(this.api.actual());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  abrir(monto: number): Observable<SesionCajaResponse> {
    return this.api.abrir(monto).pipe(
      tap((sesion) => {
        this.sesion.set(sesion);
        this.sesion.load(this.api.actual());
      }),
    );
  }

  cerrar(request: CierreCajaRequest): Observable<SesionCajaResponse> {
    return this.api.cerrar(request).pipe(
      tap(() => {
        this.sesion.reset();
        this.resumen.reset();
      }),
    );
  }

  cargarHistorial(): void {
    this.historial.load(this.api.historial());
  }

  cargarPendientes(q: string | null = null): void {
    this.pendientes.load(this.api.pendientes(q));
  }

  cobrar(request: CobroRequest): Observable<ComprobanteResponse> {
    return this.api.cobrar(request).pipe(tap(() => this.cargarResumen()));
  }

  cargarComprobantes(
    filtro: {
      desde?: string | null;
      hasta?: string | null;
      q?: string | null;
      estado?: string | null;
    } = {},
  ): void {
    this.comprobantes.load(this.api.comprobantes(filtro));
  }

  anular(id: string, motivo: string): Observable<ComprobanteResponse> {
    return this.api
      .anular(id, motivo)
      .pipe(
        tap((comprobante) =>
          this.comprobantes.update((items) =>
            items?.map((item) => (item.id === id ? comprobante : item)),
          ),
        ),
      );
  }

  cargarReporte(desde: string, hasta: string): void {
    this.reporte.load(this.api.reporteIngresos(desde, hasta));
  }

  reset(): void {
    this.sesion.reset();
    this.resumen.reset();
    this.historial.reset();
    this.pendientes.reset();
    this.comprobantes.reset();
    this.reporte.reset();
  }
}
