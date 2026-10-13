import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CitaCreateRequest,
  CitaResponse,
  ESTADOS_CITA,
  ESTADOS_CITA_ACTIVOS,
  EstadoCita,
  RangoFechas,
} from '@tusalud/app/core/models';
import { CitasApiService } from '@tusalud/app/core/services/api/citas-api.service';
import { MedicosApiService } from '@tusalud/app/core/services/api/medicos-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

function porFecha(a: CitaResponse, b: CitaResponse): number {
  return a.fechaHora.localeCompare(b.fechaHora);
}

function reemplazar(
  items: CitaResponse[] | undefined,
  cita: CitaResponse,
): CitaResponse[] | undefined {
  return items?.map((item) => (item.id === cita.id ? cita : item));
}

export function esCitaActiva(cita: CitaResponse): boolean {
  return ESTADOS_CITA_ACTIVOS.includes(cita.estado);
}

@Injectable({ providedIn: 'root' })
export class CitasStore {
  private readonly api = inject(CitasApiService);
  private readonly medicosApi = inject(MedicosApiService);
  private readonly rangoActual = signal<RangoFechas>({});
  private readonly fechaAgendaActual = signal<string | null>(null);

  readonly misCitas = new AsyncState<CitaResponse[]>();
  readonly agenda = new AsyncState<CitaResponse[]>();
  readonly selected = new AsyncState<CitaResponse>();

  readonly rango = this.rangoActual.asReadonly();
  readonly fechaAgenda = this.fechaAgendaActual.asReadonly();
  readonly citas = computed(() => [...(this.misCitas.value() ?? [])].sort(porFecha));
  readonly proximas = computed(() => this.citas().filter(esCitaActiva));
  readonly historial = computed(() => this.citas().filter((cita) => !esCitaActiva(cita)));
  readonly citasAgenda = computed(() => [...(this.agenda.value() ?? [])].sort(porFecha));
  readonly conteoPorEstado = computed<Readonly<Record<EstadoCita, number>>>(() => {
    const conteo = Object.fromEntries(ESTADOS_CITA.map((estado) => [estado, 0])) as Record<
      EstadoCita,
      number
    >;
    this.citas().forEach((cita) => conteo[cita.estado]++);
    return conteo;
  });

  constructor() {
    resetOnLogout(() => this.reset());
  }

  loadMisCitas(rango: RangoFechas = {}): void {
    this.rangoActual.set(rango);
    this.misCitas.load(this.api.misCitas(rango));
  }

  loadAgenda(fecha: string | null = null): void {
    this.fechaAgendaActual.set(fecha);
    this.agenda.load(this.medicosApi.miAgenda(fecha));
  }

  loadOne(id: number): void {
    this.selected.load(this.api.obtener(id));
  }

  crear(request: CitaCreateRequest): Observable<CitaResponse> {
    return this.api.crear(request).pipe(
      tap((cita) => {
        if (this.misCitas.hasValue()) {
          this.misCitas.update((items) => [...(items ?? []), cita]);
        }
      }),
    );
  }

  cancelar(id: number): Observable<CitaResponse> {
    return this.api.cancelar(id).pipe(tap((cita) => this.sincronizar(cita)));
  }

  confirmar(id: number): Observable<CitaResponse> {
    return this.api.confirmar(id).pipe(tap((cita) => this.sincronizar(cita)));
  }

  atender(id: number): Observable<CitaResponse> {
    return this.api.atender(id).pipe(tap((cita) => this.sincronizar(cita)));
  }

  reset(): void {
    this.rangoActual.set({});
    this.fechaAgendaActual.set(null);
    this.misCitas.reset();
    this.agenda.reset();
    this.selected.reset();
  }

  private sincronizar(cita: CitaResponse): void {
    this.misCitas.update((items) => reemplazar(items, cita));
    this.agenda.update((items) => reemplazar(items, cita));
    if (this.selected.value()?.id === cita.id) {
      this.selected.set(cita);
    }
  }
}
