import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AtencionRequest,
  AtencionResponse,
  CompartidoResponse,
  CompartirPacienteRequest,
  DatosMedicosRequest,
  HistoriaClinicaResponse,
  PacienteClinicoResponse,
  PacienteResumenResponse,
  ValoracionRequest,
  ValoracionResponse,
  ValoracionesMedicoResponse,
} from '@tusalud/app/core/models';
import { ClinicaApiService } from '@tusalud/app/core/services/api/clinica-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class ClinicaStore {
  private readonly api = inject(ClinicaApiService);

  readonly historia = new AsyncState<HistoriaClinicaResponse>();
  readonly pacientes = new AsyncState<PacienteResumenResponse[]>();
  readonly atencion = new AsyncState<AtencionResponse>();
  readonly compartidos = new AsyncState<CompartidoResponse[]>();
  readonly valoracion = new AsyncState<ValoracionResponse>();
  readonly valoraciones = new AsyncState<ValoracionesMedicoResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarMiHistoria(): void {
    this.historia.load(this.api.miHistoria());
  }

  cargarHistoriaDePaciente(pacienteId: number): void {
    this.historia.load(this.api.historiaDePaciente(pacienteId));
  }

  guardarDatosMedicos(request: DatosMedicosRequest): Observable<PacienteClinicoResponse> {
    return this.api
      .actualizarDatosMedicos(request)
      .pipe(
        tap((paciente) =>
          this.historia.update((historia) => (historia ? { ...historia, paciente } : historia)),
        ),
      );
  }

  cargarCompartidos(pacienteId: number): void {
    this.compartidos.load(this.api.listarCompartidos(pacienteId));
  }

  compartirPaciente(
    pacienteId: number,
    request: CompartirPacienteRequest,
  ): Observable<CompartidoResponse> {
    return this.api
      .compartirPaciente(pacienteId, request)
      .pipe(
        tap((compartido) => this.compartidos.update((items) => [...(items ?? []), compartido])),
      );
  }

  dejarDeCompartir(pacienteId: number, medicoId: number): Observable<void> {
    return this.api
      .dejarDeCompartir(pacienteId, medicoId)
      .pipe(
        tap(() =>
          this.compartidos.update((items) => items?.filter((item) => item.medicoId !== medicoId)),
        ),
      );
  }

  cargarMisPacientes(): void {
    this.pacientes.load(this.api.misPacientes());
  }

  cargarAtencion(citaId: number): void {
    this.atencion.load(this.api.obtenerAtencion(citaId));
  }

  registrarAtencion(citaId: number, request: AtencionRequest): Observable<AtencionResponse> {
    return this.api
      .registrarAtencion(citaId, request)
      .pipe(tap((atencion) => this.atencion.set(atencion)));
  }

  actualizarAtencion(citaId: number, request: AtencionRequest): Observable<AtencionResponse> {
    return this.api
      .actualizarAtencion(citaId, request)
      .pipe(tap((atencion) => this.atencion.set(atencion)));
  }

  cargarValoracion(citaId: number): void {
    this.valoracion.load(this.api.obtenerValoracion(citaId));
  }

  valorar(citaId: number, request: ValoracionRequest): Observable<ValoracionResponse> {
    return this.api
      .valorar(citaId, request)
      .pipe(tap((valoracion) => this.valoracion.set(valoracion)));
  }

  cargarValoracionesDeMedico(medicoId: number): void {
    this.valoraciones.load(this.api.valoracionesDeMedico(medicoId));
  }

  reset(): void {
    this.historia.reset();
    this.pacientes.reset();
    this.compartidos.reset();
    this.atencion.reset();
    this.valoracion.reset();
    this.valoraciones.reset();
  }
}
