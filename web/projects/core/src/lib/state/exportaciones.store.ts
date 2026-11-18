import { Injectable, inject } from '@angular/core';
import {
  Observable,
  catchError,
  filter,
  map,
  of,
  switchMap,
  take,
  tap,
  throwError,
  timeout,
  timer,
} from 'rxjs';

import { EstadoExportacion, ExportacionRequest, ExportacionResponse } from '../models';
import { ExportacionesApiService } from '../services/api/exportaciones-api.service';
import { DescargaService } from '../services/descarga.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

const TERMINALES: readonly EstadoExportacion[] = ['LISTA', 'ERROR', 'EXPIRADA'];
const INTERVALO_MS = 1200;
const ESPERA_MAXIMA_MS = 60_000;

export type ResultadoExportacion =
  | { estado: 'descargada'; exportacion: ExportacionResponse }
  | { estado: 'en-proceso'; exportacion: ExportacionResponse }
  | { estado: 'error'; exportacion: ExportacionResponse };

class ExportacionTardia extends Error {}

export function esTerminal(estado: EstadoExportacion): boolean {
  return TERMINALES.includes(estado);
}

@Injectable({ providedIn: 'root' })
export class ExportacionesStore {
  private readonly api = inject(ExportacionesApiService);
  private readonly descargas = inject(DescargaService);

  readonly lista = new AsyncState<ExportacionResponse[]>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargar(): void {
    this.lista.load(this.api.mias());
  }

  refrescar(): void {
    this.api.mias().subscribe({
      next: (items) => this.lista.set(items),
      error: () => undefined,
    });
  }

  solicitarYEsperar(request: ExportacionRequest): Observable<ResultadoExportacion> {
    return this.api.solicitar(request).pipe(
      tap(() => this.refrescar()),
      switchMap((exportacion) =>
        this.esperar(exportacion).pipe(
          switchMap((final): Observable<ResultadoExportacion> => {
            if (final.estado !== 'LISTA') {
              return of({ estado: 'error', exportacion: final });
            }
            return this.api.descargar(final.id).pipe(
              tap((archivo) => this.descargas.guardar(archivo)),
              map((): ResultadoExportacion => ({ estado: 'descargada', exportacion: final })),
            );
          }),
          catchError((error: unknown) =>
            error instanceof ExportacionTardia
              ? of<ResultadoExportacion>({ estado: 'en-proceso', exportacion })
              : throwError(() => error),
          ),
        ),
      ),
    );
  }

  descargar(exportacion: ExportacionResponse): Observable<void> {
    return this.api.descargar(exportacion.id).pipe(
      tap((archivo) => this.descargas.guardar(archivo)),
      map(() => undefined),
    );
  }

  eliminar(id: string): Observable<void> {
    return this.api
      .eliminar(id)
      .pipe(tap(() => this.lista.update((items) => items?.filter((item) => item.id !== id))));
  }

  reset(): void {
    this.lista.reset();
  }

  private esperar(exportacion: ExportacionResponse): Observable<ExportacionResponse> {
    if (esTerminal(exportacion.estado)) {
      return of(exportacion);
    }
    return timer(INTERVALO_MS, INTERVALO_MS).pipe(
      switchMap(() => this.api.obtener(exportacion.id)),
      filter((actual) => esTerminal(actual.estado)),
      take(1),
      timeout({
        first: ESPERA_MAXIMA_MS,
        with: () => throwError(() => new ExportacionTardia()),
      }),
    );
  }
}
