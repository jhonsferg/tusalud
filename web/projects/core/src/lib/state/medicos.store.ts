import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  DIAS_SEMANA,
  DiaSemana,
  DisponibilidadDiaResponse,
  DisponibilidadResponse,
  HorarioRequest,
  HorarioResponse,
  MedicoCreateRequest,
  MedicoResponse,
} from '../models';
import { MedicosApiService } from '../services/api/medicos-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

export interface HorariosDelDia {
  readonly dia: DiaSemana;
  readonly bloques: readonly HorarioResponse[];
}

export function ordenarHorarios(horarios: readonly HorarioResponse[]): HorarioResponse[] {
  return [...horarios].sort(
    (a, b) =>
      DIAS_SEMANA.indexOf(a.diaSemana) - DIAS_SEMANA.indexOf(b.diaSemana) ||
      a.horaInicio.localeCompare(b.horaInicio),
  );
}

function porApellido(a: MedicoResponse, b: MedicoResponse): number {
  return a.apellido.localeCompare(b.apellido, 'es') || a.nombre.localeCompare(b.nombre, 'es');
}

@Injectable({ providedIn: 'root' })
export class MedicosStore {
  private readonly api = inject(MedicosApiService);
  private readonly especialidadFiltro = signal<string | null>(null);

  readonly list = new AsyncState<MedicoResponse[]>();
  readonly selected = new AsyncState<MedicoResponse>();
  readonly horarios = new AsyncState<HorarioResponse[]>();
  readonly disponibilidad = new AsyncState<DisponibilidadResponse>();
  readonly disponibilidadRango = new AsyncState<DisponibilidadDiaResponse[]>();

  readonly filtroEspecialidad = this.especialidadFiltro.asReadonly();
  readonly medicos = computed(() => this.list.value() ?? []);
  readonly medicosActivos = computed(() => this.medicos().filter((m) => m.estado === 'ACTIVO'));
  readonly horariosOrdenados = computed(() => ordenarHorarios(this.horarios.value() ?? []));
  readonly horariosPorDia = computed<readonly HorariosDelDia[]>(() => {
    const horarios = this.horariosOrdenados();
    return DIAS_SEMANA.map((dia) => ({
      dia,
      bloques: horarios.filter((horario) => horario.diaSemana === dia),
    })).filter((grupo) => grupo.bloques.length > 0);
  });
  readonly horasDisponibles = computed(
    () => this.disponibilidad.value()?.horariosDisponibles ?? [],
  );

  constructor() {
    resetOnLogout(() => this.reset());
  }

  load(especialidadId: string | null = null): void {
    this.especialidadFiltro.set(especialidadId);
    this.list.load(this.api.listar(especialidadId));
  }

  loadOne(id: string): void {
    this.selected.load(this.api.obtener(id));
  }

  crear(request: MedicoCreateRequest): Observable<MedicoResponse> {
    return this.api.crear(request).pipe(
      tap((creado) => {
        const filtro = this.especialidadFiltro();
        if (filtro === null || creado.especialidades.some((e) => e.id === filtro)) {
          this.list.update((items) => [...(items ?? []), creado].sort(porApellido));
        }
      }),
    );
  }

  actualizarEspecialidades(id: string, especialidadIds: string[]): Observable<MedicoResponse> {
    return this.api.actualizarEspecialidades(id, especialidadIds).pipe(
      tap((actualizado) => {
        this.selected.set(actualizado);
        this.list.update((items) => items?.map((item) => (item.id === id ? actualizado : item)));
      }),
    );
  }

  loadHorarios(medicoId: string): void {
    this.horarios.load(this.api.listarHorarios(medicoId));
  }

  agregarHorario(medicoId: string, request: HorarioRequest): Observable<HorarioResponse> {
    return this.api
      .agregarHorario(medicoId, request)
      .pipe(tap((horario) => this.horarios.update((items) => [...(items ?? []), horario])));
  }

  eliminarHorario(medicoId: string, horarioId: string): Observable<void> {
    return this.api
      .eliminarHorario(medicoId, horarioId)
      .pipe(
        tap(() =>
          this.horarios.update((items) => items?.filter((horario) => horario.id !== horarioId)),
        ),
      );
  }

  loadDisponibilidad(medicoId: string, fecha: string): void {
    this.disponibilidad.load(this.api.disponibilidad(medicoId, fecha));
  }

  loadDisponibilidadRango(medicoId: string, inicio: string, fin: string): void {
    this.disponibilidadRango.load(this.api.disponibilidadRango(medicoId, inicio, fin));
  }

  reset(): void {
    this.especialidadFiltro.set(null);
    this.list.reset();
    this.selected.reset();
    this.horarios.reset();
    this.disponibilidad.reset();
    this.disponibilidadRango.reset();
  }
}
