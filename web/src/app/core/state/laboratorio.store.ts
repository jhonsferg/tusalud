import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  ExamenRequest,
  ExamenResponse,
  FiltroOrdenes,
  OrdenRequest,
  OrdenResponse,
  ResultadosRequest,
  ResumenLaboratorioResponse,
} from '@tusalud/app/core/models';
import { LaboratorioApiService } from '@tusalud/app/core/services/api/laboratorio-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class LaboratorioStore {
  private readonly api = inject(LaboratorioApiService);

  readonly resumen = new AsyncState<ResumenLaboratorioResponse>();
  readonly examenes = new AsyncState<ExamenResponse[]>();
  readonly ordenes = new AsyncState<OrdenResponse[]>();
  readonly orden = new AsyncState<OrdenResponse>();
  readonly misOrdenes = new AsyncState<OrdenResponse[]>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarExamenes(
    filtro: { q?: string | null; categoria?: string | null; activo?: boolean | null } = {},
  ): void {
    this.examenes.load(this.api.listarExamenes(filtro));
  }

  crearExamen(request: ExamenRequest): Observable<ExamenResponse> {
    return this.api
      .crearExamen(request)
      .pipe(tap((examen) => this.examenes.update((items) => [examen, ...(items ?? [])])));
  }

  actualizarExamen(id: number, request: ExamenRequest): Observable<ExamenResponse> {
    return this.api
      .actualizarExamen(id, request)
      .pipe(
        tap((examen) =>
          this.examenes.update((items) => items?.map((item) => (item.id === id ? examen : item))),
        ),
      );
  }

  cargarOrdenes(filtro: FiltroOrdenes = {}): void {
    this.ordenes.load(this.api.listarOrdenes(filtro));
  }

  cargarOrden(id: number): void {
    this.orden.load(this.api.obtenerOrden(id));
  }

  crearOrden(request: OrdenRequest): Observable<OrdenResponse> {
    return this.api.crearOrden(request);
  }

  tomarMuestra(id: number): Observable<OrdenResponse> {
    return this.api.tomarMuestra(id).pipe(tap((orden) => this.aplicar(orden)));
  }

  registrarResultados(
    id: number,
    itemId: number,
    request: ResultadosRequest,
  ): Observable<OrdenResponse> {
    return this.api
      .registrarResultados(id, itemId, request)
      .pipe(tap((orden) => this.aplicar(orden)));
  }

  validar(id: number): Observable<OrdenResponse> {
    return this.api.validar(id).pipe(tap((orden) => this.aplicar(orden)));
  }

  cancelar(id: number, motivo?: string | null): Observable<OrdenResponse> {
    return this.api.cancelar(id, motivo).pipe(tap((orden) => this.aplicar(orden)));
  }

  cargarMisOrdenes(): void {
    this.misOrdenes.load(this.api.misOrdenes());
  }

  reset(): void {
    this.resumen.reset();
    this.examenes.reset();
    this.ordenes.reset();
    this.orden.reset();
    this.misOrdenes.reset();
  }

  private aplicar(orden: OrdenResponse): void {
    this.orden.set(orden);
    this.ordenes.update((items) => items?.map((item) => (item.id === orden.id ? orden : item)));
  }
}
