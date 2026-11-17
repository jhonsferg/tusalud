import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CitaHoyResponse,
  PantallaTurno,
  ResumenRecepcionResponse,
  TurnoRequest,
  TurnoResponse,
} from '@tusalud/app/core/models';
import { RecepcionApiService } from '@tusalud/app/core/services/api/recepcion-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class RecepcionStore {
  private readonly api = inject(RecepcionApiService);

  readonly resumen = new AsyncState<ResumenRecepcionResponse>();
  readonly citasHoy = new AsyncState<CitaHoyResponse[]>();
  readonly cola = new AsyncState<TurnoResponse[]>();
  readonly pantalla = new AsyncState<PantallaTurno[]>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarCitasHoy(q: string | null = null): void {
    this.citasHoy.load(this.api.citasHoy(q));
  }

  cargarCola(estado: string | null = null, medicoId: string | null = null): void {
    this.cola.load(this.api.cola(estado, medicoId));
  }

  cargarPantalla(): void {
    this.pantalla.load(this.api.pantalla());
  }

  registrarLlegada(citaId: string): Observable<TurnoResponse> {
    return this.api.registrarLlegada(citaId).pipe(tap(() => this.refrescar()));
  }

  crearTurno(request: TurnoRequest): Observable<TurnoResponse> {
    return this.api.crearTurno(request).pipe(tap(() => this.refrescar()));
  }

  cambiar(
    id: string,
    accion: 'llamar' | 'iniciar' | 'finalizar' | 'ausente' | 'cancelar',
  ): Observable<TurnoResponse> {
    return this.api.cambiarTurno(id, accion).pipe(
      tap((turno) => {
        this.cola.update((items) => items?.map((item) => (item.id === turno.id ? turno : item)));
        this.cargarResumen();
      }),
    );
  }

  private refrescar(): void {
    this.cargarCola();
    this.cargarCitasHoy();
    this.cargarResumen();
  }

  reset(): void {
    this.resumen.reset();
    this.citasHoy.reset();
    this.cola.reset();
    this.pantalla.reset();
  }
}
