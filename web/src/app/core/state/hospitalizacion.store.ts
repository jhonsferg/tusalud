import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
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
} from '@tusalud/app/core/models';
import { HospitalizacionApiService } from '@tusalud/app/core/services/api/hospitalizacion-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class HospitalizacionStore {
  private readonly api = inject(HospitalizacionApiService);

  readonly resumen = new AsyncState<ResumenHospResponse>();
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

  cargarIngreso(id: string): void {
    this.ingreso.load(this.api.ingreso(id));
    this.evoluciones.load(this.api.evoluciones(id));
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
