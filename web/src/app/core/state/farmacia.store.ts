import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CatalogoProductoResponse,
  DispensacionRequest,
  DispensacionResponse,
  RecetaFarmaciaResponse,
  ResumenFarmaciaResponse,
} from '@tusalud/app/core/models';
import { FarmaciaApiService } from '@tusalud/app/core/services/api/farmacia-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

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

  anular(id: number): Observable<DispensacionResponse> {
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
