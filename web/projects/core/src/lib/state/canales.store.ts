import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CanalExterno,
  ContactoCanal,
  EntregaNotificacion,
  EstadoRespaldos,
  FiltroEntregas,
  MisCanales,
  PaginaEntregas,
  PlantillaMensaje,
  PlantillaMensajeRequest,
  PlantillasMensaje,
  PreferenciasCanalesRequest,
  ProveedoresCanales,
  RespaldoEjecucion,
  ResumenEntregas,
  SolicitudContactoRequest,
  TipoEjecucionRespaldo,
} from '../models';
import { CanalesApiService } from '../services/api/canales-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class CanalesStore {
  private readonly api = inject(CanalesApiService);

  readonly mis = new AsyncState<MisCanales>();
  readonly entregas = new AsyncState<PaginaEntregas>();
  readonly resumen = new AsyncState<ResumenEntregas>();
  readonly plantillas = new AsyncState<PlantillasMensaje>();
  readonly proveedores = new AsyncState<ProveedoresCanales>();
  readonly respaldos = new AsyncState<RespaldoEjecucion[]>();
  readonly estadoRespaldos = new AsyncState<EstadoRespaldos>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarMis(): void {
    this.mis.load(this.api.misCanales());
  }

  solicitar(canal: CanalExterno, request: SolicitudContactoRequest): Observable<ContactoCanal> {
    return this.api.solicitar(canal, request).pipe(tap(() => this.cargarMis()));
  }

  verificar(canal: CanalExterno, codigo: string): Observable<ContactoCanal> {
    return this.api.verificar(canal, codigo).pipe(tap(() => this.cargarMis()));
  }

  revocar(canal: CanalExterno): Observable<ContactoCanal> {
    return this.api.revocar(canal).pipe(tap(() => this.cargarMis()));
  }

  guardarPreferencias(request: PreferenciasCanalesRequest): Observable<MisCanales> {
    return this.api.guardarPreferencias(request).pipe(tap((mis) => this.mis.set(mis)));
  }

  cargarEntregas(filtro: FiltroEntregas = {}): void {
    this.entregas.load(this.api.entregas(filtro));
  }

  cargarResumen(dias = 7): void {
    this.resumen.load(this.api.resumenEntregas(dias));
  }

  reintentar(id: string): Observable<EntregaNotificacion> {
    return this.api.reintentar(id);
  }

  cargarPlantillas(): void {
    this.plantillas.load(this.api.plantillas());
  }

  actualizarPlantilla(
    plantilla: PlantillaMensaje,
    request: PlantillaMensajeRequest,
  ): Observable<PlantillaMensaje> {
    return this.api
      .actualizarPlantilla(plantilla.tipo, plantilla.canal, request)
      .pipe(tap(() => this.cargarPlantillas()));
  }

  restaurarPlantilla(plantilla: PlantillaMensaje): Observable<PlantillaMensaje> {
    return this.api
      .restaurarPlantilla(plantilla.tipo, plantilla.canal)
      .pipe(tap(() => this.cargarPlantillas()));
  }

  historialPlantilla(plantilla: PlantillaMensaje): Observable<PlantillaMensaje[]> {
    return this.api.historialPlantilla(plantilla.tipo, plantilla.canal);
  }

  cargarProveedores(): void {
    this.proveedores.load(this.api.proveedores());
  }

  cargarRespaldos(tipo: TipoEjecucionRespaldo | null = null): void {
    this.respaldos.load(this.api.respaldos(tipo));
  }

  cargarEstadoRespaldos(): void {
    this.estadoRespaldos.load(this.api.estadoRespaldos());
  }

  reset(): void {
    this.mis.reset();
    this.entregas.reset();
    this.resumen.reset();
    this.plantillas.reset();
    this.proveedores.reset();
    this.respaldos.reset();
    this.estadoRespaldos.reset();
  }
}
