import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AprobarPreautorizacionRequest,
  AseguradoraRequest,
  AseguradoraResponse,
  CarteraResponse,
  ConvenioRequest,
  ConvenioResponse,
  EstadoPreautorizacion,
  LiquidacionCrearRequest,
  LiquidacionPagoRequest,
  LiquidacionResponse,
  MiPolizaResponse,
  PreautorizacionRequest,
  PreautorizacionResponse,
  SegurosResumenResponse,
} from '../models';
import { SegurosApiService } from '../services/api/seguros-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class SegurosStore {
  private readonly api = inject(SegurosApiService);

  readonly aseguradoras = new AsyncState<AseguradoraResponse[]>();
  readonly convenios = new AsyncState<ConvenioResponse[]>();
  readonly cartera = new AsyncState<CarteraResponse[]>();
  readonly liquidaciones = new AsyncState<LiquidacionResponse[]>();
  readonly preautorizaciones = new AsyncState<PreautorizacionResponse[]>();
  readonly misPolizas = new AsyncState<MiPolizaResponse[]>();
  readonly misPreautorizaciones = new AsyncState<PreautorizacionResponse[]>();
  readonly resumen = new AsyncState<SegurosResumenResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarCatalogo(): void {
    this.aseguradoras.load(this.api.aseguradoras());
    this.convenios.load(this.api.convenios());
  }

  guardarAseguradora(
    id: string | null,
    request: AseguradoraRequest,
  ): Observable<AseguradoraResponse> {
    const operacion = id
      ? this.api.actualizarAseguradora(id, request)
      : this.api.crearAseguradora(request);
    return operacion.pipe(tap(() => this.cargarCatalogo()));
  }

  guardarConvenio(id: string | null, request: ConvenioRequest): Observable<ConvenioResponse> {
    const operacion = id
      ? this.api.actualizarConvenio(id, request)
      : this.api.crearConvenio(request);
    return operacion.pipe(tap(() => this.cargarCatalogo()));
  }

  cargarCartera(): void {
    this.cartera.load(this.api.cartera());
  }

  cargarLiquidaciones(): void {
    this.liquidaciones.load(this.api.liquidaciones());
  }

  crearLiquidacion(request: LiquidacionCrearRequest): Observable<LiquidacionResponse> {
    return this.api.crearLiquidacion(request).pipe(
      tap(() => {
        this.cargarLiquidaciones();
        this.cargarCartera();
      }),
    );
  }

  enviarLiquidacion(id: string): Observable<LiquidacionResponse> {
    return this.api.enviarLiquidacion(id).pipe(tap(() => this.cargarLiquidaciones()));
  }

  registrarPago(id: string, request: LiquidacionPagoRequest): Observable<LiquidacionResponse> {
    return this.api
      .registrarPagoLiquidacion(id, request)
      .pipe(tap(() => this.cargarLiquidaciones()));
  }

  cargarPreautorizaciones(estado: EstadoPreautorizacion | null = null): void {
    this.preautorizaciones.load(this.api.preautorizaciones(estado));
  }

  aprobar(id: string, request: AprobarPreautorizacionRequest): Observable<PreautorizacionResponse> {
    return this.api.aprobarPreautorizacion(id, request);
  }

  rechazar(id: string, motivo: string): Observable<PreautorizacionResponse> {
    return this.api.rechazarPreautorizacion(id, motivo);
  }

  cargarMisPolizas(): void {
    this.misPolizas.load(this.api.misPolizas());
  }

  cargarMisPreautorizaciones(): void {
    this.misPreautorizaciones.load(this.api.misPreautorizaciones());
  }

  solicitar(request: PreautorizacionRequest): Observable<PreautorizacionResponse> {
    return this.api
      .solicitarPreautorizacion(request)
      .pipe(tap(() => this.cargarMisPreautorizaciones()));
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  reset(): void {
    this.aseguradoras.reset();
    this.convenios.reset();
    this.cartera.reset();
    this.liquidaciones.reset();
    this.preautorizaciones.reset();
    this.misPolizas.reset();
    this.misPreautorizaciones.reset();
    this.resumen.reset();
  }
}
