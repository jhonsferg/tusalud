import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AusenciaRequest,
  AusenciaResponse,
  EmpleadoRequest,
  EmpleadoResponse,
  FiltroAusencias,
  FiltroEmpleados,
  MisAusenciasResponse,
  PlantillaTurnoResponse,
  ResumenRrhhResponse,
  SemanaTurnosResponse,
  TurnoPersonalRequest,
  TurnoPersonalResponse,
  UsuarioDisponibleResponse,
} from '../models';
import { RrhhApiService } from '../services/api/rrhh-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class RrhhStore {
  private readonly api = inject(RrhhApiService);

  readonly resumen = new AsyncState<ResumenRrhhResponse>();
  readonly empleados = new AsyncState<EmpleadoResponse[]>();
  readonly usuariosDisponibles = new AsyncState<UsuarioDisponibleResponse[]>();
  readonly plantillas = new AsyncState<PlantillaTurnoResponse[]>();
  readonly semana = new AsyncState<SemanaTurnosResponse>();
  readonly ausencias = new AsyncState<AusenciaResponse[]>();
  readonly miFicha = new AsyncState<EmpleadoResponse>();
  readonly misTurnos = new AsyncState<TurnoPersonalResponse[]>();
  readonly misAusencias = new AsyncState<MisAusenciasResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarEmpleados(filtro: FiltroEmpleados = {}): void {
    this.empleados.load(this.api.empleados(filtro));
  }

  cargarUsuariosDisponibles(): void {
    this.usuariosDisponibles.load(this.api.usuariosDisponibles());
  }

  crearEmpleado(request: EmpleadoRequest): Observable<EmpleadoResponse> {
    return this.api.crearEmpleado(request);
  }

  actualizarEmpleado(id: string, request: EmpleadoRequest): Observable<EmpleadoResponse> {
    return this.api.actualizarEmpleado(id, request);
  }

  cargarPlantillas(): void {
    this.plantillas.load(this.api.plantillasTurno());
  }

  cargarSemana(fecha: string, area?: string | null): void {
    this.semana.load(this.api.semana(fecha, area));
  }

  asignarTurno(request: TurnoPersonalRequest): Observable<TurnoPersonalResponse> {
    return this.api.asignarTurno(request);
  }

  eliminarTurno(id: string): Observable<void> {
    return this.api.eliminarTurno(id);
  }

  cargarAusencias(filtro: FiltroAusencias = {}): void {
    this.ausencias.load(this.api.ausencias(filtro));
  }

  resolverAusencia(
    id: string,
    accion: 'aprobar' | 'rechazar',
    motivo: string | null,
  ): Observable<AusenciaResponse> {
    return this.api
      .resolverAusencia(id, accion, motivo)
      .pipe(
        tap((ausencia) =>
          this.ausencias.update((lista) => lista?.map((a) => (a.id === id ? ausencia : a))),
        ),
      );
  }

  cargarMiFicha(): void {
    this.miFicha.load(this.api.miFicha());
  }

  cargarMisTurnos(desde: string, hasta: string): void {
    this.misTurnos.load(this.api.misTurnos(desde, hasta));
  }

  cargarMisAusencias(): void {
    this.misAusencias.load(this.api.misAusencias());
  }

  solicitarAusencia(request: AusenciaRequest): Observable<AusenciaResponse> {
    return this.api.solicitarAusencia(request).pipe(tap(() => this.cargarMisAusencias()));
  }

  cancelarMiAusencia(id: string): Observable<AusenciaResponse> {
    return this.api.cancelarMiAusencia(id).pipe(tap(() => this.cargarMisAusencias()));
  }

  reset(): void {
    this.resumen.reset();
    this.empleados.reset();
    this.usuariosDisponibles.reset();
    this.plantillas.reset();
    this.semana.reset();
    this.ausencias.reset();
    this.miFicha.reset();
    this.misTurnos.reset();
    this.misAusencias.reset();
  }
}
