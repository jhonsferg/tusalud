import { Injectable, computed, inject, signal } from '@angular/core';

import {
  CitaResponse,
  CitasPorEstadoResponse,
  ESTADOS_CITA,
  RangoFechas,
} from '@tusalud/app/core/models';
import { AuditoriaApiService } from '@tusalud/app/core/services/api/auditoria-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class AuditoriaStore {
  private readonly api = inject(AuditoriaApiService);
  private readonly rangoActual = signal<RangoFechas>({});

  readonly porEstado = new AsyncState<CitasPorEstadoResponse[]>();
  readonly citas = new AsyncState<CitaResponse[]>();

  readonly rango = this.rangoActual.asReadonly();
  readonly resumen = computed<readonly CitasPorEstadoResponse[]>(() => {
    const datos = this.porEstado.value() ?? [];
    return ESTADOS_CITA.map((estado) => ({
      estado,
      cantidad: datos.find((item) => item.estado === estado)?.cantidad ?? 0,
    }));
  });
  readonly totalCitas = computed(() =>
    this.resumen().reduce((total, item) => total + item.cantidad, 0),
  );
  readonly ingresosAtendidos = computed(() =>
    (this.citas.value() ?? [])
      .filter((cita) => cita.estado === 'ATENDIDA')
      .reduce((total, cita) => total + cita.monto, 0),
  );

  constructor() {
    resetOnLogout(() => this.reset());
  }

  loadPorEstado(): void {
    this.porEstado.load(this.api.citasPorEstado());
  }

  loadCitas(rango: RangoFechas = {}): void {
    this.rangoActual.set(rango);
    this.citas.load(this.api.citas(rango));
  }

  reset(): void {
    this.rangoActual.set({});
    this.porEstado.reset();
    this.citas.reset();
  }
}
