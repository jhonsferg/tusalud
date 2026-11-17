import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  EventoCalendarioRequest,
  EventoCalendarioResponse,
  MoverEventoRequest,
} from '@tusalud/app/core/models';
import { CalendarioApiService } from '@tusalud/app/core/services/api/calendario-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class CalendarioStore {
  private readonly api = inject(CalendarioApiService);

  readonly eventos = new AsyncState<EventoCalendarioResponse[]>();

  constructor() {
    resetOnLogout(() => this.eventos.reset());
  }

  cargar(inicio: string, fin: string): void {
    this.eventos.load(this.api.listar(inicio, fin));
  }

  crear(request: EventoCalendarioRequest): Observable<EventoCalendarioResponse> {
    return this.api
      .crear(request)
      .pipe(tap((evento) => this.eventos.update((items) => [...(items ?? []), evento])));
  }

  actualizar(id: number, request: EventoCalendarioRequest): Observable<EventoCalendarioResponse> {
    return this.api.actualizar(id, request).pipe(tap((evento) => this.reemplazar(evento)));
  }

  mover(id: number, request: MoverEventoRequest): Observable<EventoCalendarioResponse> {
    return this.api.mover(id, request).pipe(tap((evento) => this.reemplazar(evento)));
  }

  completar(id: number, completado: boolean): Observable<EventoCalendarioResponse> {
    return this.api.completar(id, completado).pipe(tap((evento) => this.reemplazar(evento)));
  }

  eliminar(id: number): Observable<void> {
    return this.api
      .eliminar(id)
      .pipe(tap(() => this.eventos.update((items) => items?.filter((item) => item.id !== id))));
  }

  reemplazarLocal(evento: EventoCalendarioResponse): void {
    this.reemplazar(evento);
  }

  private reemplazar(evento: EventoCalendarioResponse): void {
    this.eventos.update((items) => items?.map((item) => (item.id === evento.id ? evento : item)));
  }
}
