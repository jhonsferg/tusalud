import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CatalogoProductoResponse,
  DispensacionRequest,
  DispensacionResponse,
  RecetaFarmaciaResponse,
  ResumenFarmaciaResponse,
} from '../models';
import { FarmaciaApiService } from '../services/api/farmacia-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class FarmaciaStore {
  private readonly api = inject(FarmaciaApiService);

  readonly resumen = new AsyncState<ResumenFarmaciaResponse>();
  readonly recetas = new AsyncState<RecetaFarmaciaResponse[]>();
  readonly catalogo = new AsyncState<CatalogoProductoResponse[]>();
  readonly dispensaciones = new AsyncState<DispensacionResponse[]>();
  readonly misDispensaciones = new AsyncState<DispensacionResponse[]>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarRecetas(estado: string | null = null, q: string | null = null): void {
    this.recetas.load(this.api.recetas(estado, q));
  }

  cargarCatalogo(q: string | null = null): void {
    this.catalogo.load(this.api.catalogo(q));
  }

  dispensar(request: DispensacionRequest): Observable<DispensacionResponse> {
    return this.api
      .dispensar(request)
      .pipe(
        tap((dispensacion) =>
          this.dispensaciones.update((items) => [dispensacion, ...(items ?? [])]),
        ),
      );
  }

  cargarDispensaciones(
    filtro: { desde?: string | null; hasta?: string | null; q?: string | null } = {},
  ): void {
    this.dispensaciones.load(this.api.listarDispensaciones(filtro));
  }

  anular(id: string): Observable<DispensacionResponse> {
    return this.api
      .anular(id)
      .pipe(
        tap((anulada) =>
          this.dispensaciones.update((items) =>
            items?.map((item) => (item.id === id ? anulada : item)),
          ),
        ),
      );
  }

  cargarMisDispensaciones(): void {
    this.misDispensaciones.load(this.api.misDispensaciones());
  }

  reset(): void {
    this.resumen.reset();
    this.recetas.reset();
    this.catalogo.reset();
    this.dispensaciones.reset();
    this.misDispensaciones.reset();
  }
}
