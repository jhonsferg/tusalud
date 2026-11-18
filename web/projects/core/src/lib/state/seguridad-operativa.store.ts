import { Injectable, inject } from '@angular/core';

import {
  EstadoSistemaResponse,
  FiltroEventosSeguridad,
  PaginaEventosSeguridadResponse,
  ResumenSeguridadResponse,
  UsoAlmacenamientoResponse,
} from '../models';
import { SeguridadOperativaApiService } from '../services/api/seguridad-operativa-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class SeguridadOperativaStore {
  private readonly api = inject(SeguridadOperativaApiService);

  readonly resumen = new AsyncState<ResumenSeguridadResponse>();
  readonly eventos = new AsyncState<PaginaEventosSeguridadResponse>();
  readonly estado = new AsyncState<EstadoSistemaResponse>();
  readonly uso = new AsyncState<UsoAlmacenamientoResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarEventos(filtro: FiltroEventosSeguridad = {}): void {
    this.eventos.load(this.api.eventos(filtro));
  }

  cargarEstado(): void {
    this.estado.load(this.api.estadoSistema());
  }

  cargarUso(): void {
    this.uso.load(this.api.usoAlmacenamiento());
  }

  reset(): void {
    this.resumen.reset();
    this.eventos.reset();
    this.estado.reset();
    this.uso.reset();
  }
}
