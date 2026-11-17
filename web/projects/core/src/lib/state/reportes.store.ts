import { Injectable, inject } from '@angular/core';

import {
  FiltroReporte,
  ReporteCalidadResponse,
  ReporteEjecutivoResponse,
  ReporteFinancieroResponse,
  ReporteOperativoResponse,
} from '../models';
import { ReportesApiService } from '../services/api/reportes-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class ReportesStore {
  private readonly api = inject(ReportesApiService);

  readonly ejecutivo = new AsyncState<ReporteEjecutivoResponse>();
  readonly financiero = new AsyncState<ReporteFinancieroResponse>();
  readonly operativo = new AsyncState<ReporteOperativoResponse>();
  readonly calidad = new AsyncState<ReporteCalidadResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarEjecutivo(filtro: FiltroReporte): void {
    this.ejecutivo.load(this.api.ejecutivo(filtro));
  }

  cargarFinanciero(filtro: FiltroReporte): void {
    this.financiero.load(this.api.financiero(filtro));
  }

  cargarOperativo(filtro: FiltroReporte): void {
    this.operativo.load(this.api.operativo(filtro));
  }

  cargarCalidad(filtro: FiltroReporte): void {
    this.calidad.load(this.api.calidad(filtro));
  }

  reset(): void {
    this.ejecutivo.reset();
    this.financiero.reset();
    this.operativo.reset();
    this.calidad.reset();
  }
}
