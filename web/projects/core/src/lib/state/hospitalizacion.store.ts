import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  EvolucionPendienteResponse,
  AltaRequest,
  CamaRequest,
  CamaResponse,
  EmergenciaRequest,
  EvolucionRequest,
  EvolucionResponse,
  IngresoHospRequest,
  IngresoHospResponse,
  MapaCamasResponse,
  ResumenHospResponse,
} from '../models';
import { HospitalizacionApiService } from '../services/api/hospitalizacion-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class HospitalizacionStore {
  private readonly api = inject(HospitalizacionApiService);

  readonly resumen = new AsyncState<ResumenHospResponse>();
  readonly pendientes = new AsyncState<EvolucionPendienteResponse[]>();
  readonly mapa = new AsyncState<MapaCamasResponse[]>();
  readonly camas = new AsyncState<CamaResponse[]>();
  readonly ingresos = new AsyncState<IngresoHospResponse[]>();
  readonly ingreso = new AsyncState<IngresoHospResponse>();
  readonly evoluciones = new AsyncState<EvolucionResponse[]>();
  readonly emergencias = new AsyncState<IngresoHospResponse[]>();
  readonly misIngresos = new AsyncState<IngresoHospResponse[]>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarPendientes(horas?: number | null): void {
    this.pendientes.load(this.api.evolucionesPendientes(horas));
  }

  cancelarIngreso(id: string, motivo: string): Observable<IngresoHospResponse> {
    return this.api.cancelarIngreso(id, motivo).pipe(
      tap((ingreso) => {
        this.ingreso.set(ingreso);
        this.refrescar();
      }),
    );
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarMapa(): void {
    this.mapa.load(this.api.mapa());
  }

  cargarCamas(area: string | null = null, estado: string | null = null): void {
    this.camas.load(this.api.camas(area, estado));
  }

  crearCama(request: CamaRequest): Observable<CamaResponse> {
    return this.api.crearCama(request).pipe(tap(() => this.cargarCamas()));
  }

  actualizarCama(id: string, request: CamaRequest): Observable<CamaResponse> {
    return this.api.actualizarCama(id, request).pipe(tap(() => this.cargarCamas()));
  }

  cambiarEstadoCama(id: string, estado: string): Observable<CamaResponse> {
    return this.api.estadoCama(id, estado).pipe(
      tap((cama) => {
        this.camas.update((items) => items?.map((item) => (item.id === id ? cama : item)));
        this.cargarResumen();
      }),
    );
  }

  cargarIngresos(estado: string | null = null, q: string | null = null): void {
    this.ingresos.load(this.api.ingresos(estado, q));
  }

  cargarIngreso(id: string, conEvoluciones = true): void {
    this.ingreso.load(this.api.ingreso(id));
    if (conEvoluciones) {
      this.evoluciones.load(this.api.evoluciones(id));
    } else {
      this.evoluciones.reset();
    }
  }

  crearIngreso(request: IngresoHospRequest): Observable<IngresoHospResponse> {
    return this.api.crearIngreso(request).pipe(tap(() => this.refrescar()));
  }

  trasladar(id: string, camaId: string): Observable<IngresoHospResponse> {
    return this.api.trasladar(id, camaId).pipe(tap((ingreso) => this.ingreso.set(ingreso)));
  }

  asignarCama(id: string, camaId: string): Observable<IngresoHospResponse> {
    return this.api.asignarCama(id, camaId).pipe(tap(() => this.refrescar()));
  }

  alta(id: string, request: AltaRequest): Observable<IngresoHospResponse> {
    return this.api.alta(id, request).pipe(tap((ingreso) => this.ingreso.set(ingreso)));
  }

  registrarEvolucion(id: string, request: EvolucionRequest): Observable<EvolucionResponse> {
    return this.api
      .registrarEvolucion(id, request)
      .pipe(tap((evolucion) => this.evoluciones.update((items) => [evolucion, ...(items ?? [])])));
  }

  cargarEmergencias(): void {
    this.emergencias.load(this.api.emergencias());
  }

  registrarEmergencia(request: EmergenciaRequest): Observable<IngresoHospResponse> {
    return this.api.registrarEmergencia(request).pipe(tap(() => this.refrescar()));
  }

  cargarMisIngresos(): void {
    this.misIngresos.load(this.api.misIngresos());
  }

  private refrescar(): void {
    this.cargarResumen();
    this.cargarMapa();
    this.cargarEmergencias();
  }

  reset(): void {
    this.resumen.reset();
    this.mapa.reset();
    this.camas.reset();
    this.ingresos.reset();
    this.ingreso.reset();
    this.evoluciones.reset();
    this.emergencias.reset();
    this.misIngresos.reset();
  }
}
