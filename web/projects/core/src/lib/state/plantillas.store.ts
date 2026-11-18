import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { PlantillaResponse, PlantillaVersionResponse } from '../models';
import { PlantillasApiService } from '../services/api/plantillas-api.service';
import { DescargaService } from '../services/descarga.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class PlantillasStore {
  private readonly api = inject(PlantillasApiService);
  private readonly descargas = inject(DescargaService);

  readonly lista = new AsyncState<PlantillaResponse[]>();
  readonly historial = new AsyncState<PlantillaVersionResponse[]>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargar(): void {
    this.lista.load(this.api.listar());
  }

  cargarHistorial(codigo: string): void {
    this.historial.reset();
    this.historial.load(this.api.historial(codigo));
  }

  descargar(codigo: string): Observable<void> {
    return this.api.descargar(codigo).pipe(
      tap((archivo) => this.descargas.guardar(archivo)),
      map(() => undefined),
    );
  }

  reemplazar(codigo: string, archivo: File, motivo: string | null): Observable<PlantillaResponse> {
    return this.api
      .reemplazar(codigo, archivo, motivo)
      .pipe(tap((nueva) => this.actualizar(nueva)));
  }

  restaurar(codigo: string, motivo: string | null): Observable<PlantillaResponse> {
    return this.api.restaurar(codigo, motivo).pipe(tap((nueva) => this.actualizar(nueva)));
  }

  reset(): void {
    this.lista.reset();
    this.historial.reset();
  }

  private actualizar(nueva: PlantillaResponse): void {
    this.lista.update((items) =>
      items?.map((item) => (item.codigo === nueva.codigo ? nueva : item)),
    );
  }
}
