import { Injectable, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { EspecialidadRequest, EspecialidadResponse } from '@tusalud/app/core/models';
import { EspecialidadesApiService } from '@tusalud/app/core/services/api/especialidades-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

function porNombre(a: EspecialidadResponse, b: EspecialidadResponse): number {
  return a.nombre.localeCompare(b.nombre, 'es');
}

@Injectable({ providedIn: 'root' })
export class EspecialidadesStore {
  private readonly api = inject(EspecialidadesApiService);

  readonly list = new AsyncState<EspecialidadResponse[]>();
  readonly selected = new AsyncState<EspecialidadResponse>();

  readonly especialidades = computed(() => this.list.value() ?? []);
  readonly loading = computed(() => this.list.loading() || this.selected.loading());
  readonly opciones = computed(() =>
    this.especialidades().map(({ id, nombre }) => ({ value: id, label: nombre })),
  );

  constructor() {
    resetOnLogout(() => this.reset());
  }

  load(): void {
    this.list.load(this.api.listar());
  }

  loadOne(id: number): void {
    this.selected.load(this.api.obtener(id));
  }

  crear(request: EspecialidadRequest): Observable<EspecialidadResponse> {
    return this.api
      .crear(request)
      .pipe(
        tap((creada) => this.list.update((items) => [...(items ?? []), creada].sort(porNombre))),
      );
  }

  actualizar(id: number, request: EspecialidadRequest): Observable<EspecialidadResponse> {
    return this.api.actualizar(id, request).pipe(
      tap((actualizada) => {
        this.list.update((items) =>
          items?.map((item) => (item.id === id ? actualizada : item)).sort(porNombre),
        );
        if (this.selected.value()?.id === id) {
          this.selected.set(actualizada);
        }
      }),
    );
  }

  eliminar(id: number): Observable<void> {
    return this.api.eliminar(id).pipe(
      tap(() => {
        this.list.update((items) => items?.filter((item) => item.id !== id));
        if (this.selected.value()?.id === id) {
          this.selected.reset();
        }
      }),
    );
  }

  reset(): void {
    this.list.reset();
    this.selected.reset();
  }
}
