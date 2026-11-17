import { DestroyRef, Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, Subscription, tap, timer } from 'rxjs';

import { NotificacionResponse } from '@tusalud/app/core/models';
import { NotificacionesApiService } from '@tusalud/app/core/services/api/notificaciones-api.service';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { RealtimeService } from '@tusalud/app/core/services/realtime.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { CitasStore } from '@tusalud/app/core/state/citas.store';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

const FALLBACK_POLL_MS = 120_000;

@Injectable({ providedIn: 'root' })
export class NotificacionesStore {
  private readonly api = inject(NotificacionesApiService);
  private readonly auth = inject(AuthService);
  private readonly realtime = inject(RealtimeService);
  private readonly aviso = inject(NotificationService);
  private readonly citas = inject(CitasStore);
  private readonly noLeidasState = signal(0);
  private fallback: Subscription | null = null;
  private eventos: Subscription | null = null;

  readonly lista = new AsyncState<NotificacionResponse[]>();
  readonly noLeidas = this.noLeidasState.asReadonly();
  readonly enVivo = this.realtime.connected;
  readonly items = computed(() => this.lista.value() ?? []);
  readonly recientes = computed(() => this.items().slice(0, 6));

  constructor() {
    resetOnLogout(() => this.reset());
    effect(() => {
      const autenticado = this.auth.isAuthenticated();
      untracked(() => (autenticado ? this.iniciar() : this.detener()));
    });
    inject(DestroyRef).onDestroy(() => this.detener());
  }

  cargar(soloNoLeidas = false): void {
    this.lista.load(this.api.listar(soloNoLeidas));
  }

  refrescarContador(): void {
    this.api.resumen().subscribe({
      next: (resumen) => this.noLeidasState.set(resumen.noLeidas),
      error: () => undefined,
    });
  }

  marcarLeida(id: number): Observable<NotificacionResponse> {
    return this.api.marcarLeida(id).pipe(
      tap((actualizada) => {
        const previa = this.items().find((item) => item.id === id);
        this.lista.update((items) => items?.map((item) => (item.id === id ? actualizada : item)));
        if (previa && !previa.leida) {
          this.noLeidasState.update((n) => Math.max(0, n - 1));
        }
      }),
    );
  }

  marcarTodasLeidas(): Observable<void> {
    return this.api.marcarTodasLeidas().pipe(
      tap(() => {
        this.lista.update((items) => items?.map((item) => ({ ...item, leida: true })));
        this.noLeidasState.set(0);
      }),
    );
  }

  eliminar(id: number): Observable<void> {
    return this.api.eliminar(id).pipe(
      tap(() => {
        const previa = this.items().find((item) => item.id === id);
        this.lista.update((items) => items?.filter((item) => item.id !== id));
        if (previa && !previa.leida) {
          this.noLeidasState.update((n) => Math.max(0, n - 1));
        }
      }),
    );
  }

  reset(): void {
    this.noLeidasState.set(0);
    this.lista.reset();
  }

  private iniciar(): void {
    this.detener();
    this.realtime.connect();
    this.refrescarContador();
    this.eventos = this.realtime.events$.subscribe(({ event, data }) => {
      if (event === 'conectado') {
        this.refrescarContador();
        if (this.lista.hasValue()) {
          this.cargar();
        }
      } else if (event === 'notificacion') {
        this.recibir(data);
      }
    });
    this.fallback = timer(FALLBACK_POLL_MS, FALLBACK_POLL_MS).subscribe(() => {
      if (!this.realtime.connected()) {
        this.refrescarContador();
      }
    });
  }

  private detener(): void {
    this.realtime.disconnect();
    this.eventos?.unsubscribe();
    this.fallback?.unsubscribe();
    this.eventos = null;
    this.fallback = null;
  }

  private recibir(datos: string): void {
    let notificacion: NotificacionResponse;
    try {
      notificacion = JSON.parse(datos) as NotificacionResponse;
    } catch {
      return;
    }
    if (this.items().some((item) => item.id === notificacion.id)) {
      return;
    }
    this.lista.update((items) => (items ? [notificacion, ...items] : items));
    if (!notificacion.leida) {
      this.noLeidasState.update((n) => n + 1);
    }
    this.aviso.info(`${notificacion.titulo}. ${notificacion.mensaje}`, 7000);
    if (notificacion.tipo.startsWith('CITA_') || notificacion.tipo === 'PAGO_REEMBOLSADO') {
      this.sincronizarCitas(notificacion);
    }
  }

  private sincronizarCitas(notificacion: NotificacionResponse): void {
    if (this.citas.misCitas.hasValue()) {
      this.citas.loadMisCitas(this.citas.rango());
    }
    if (this.citas.agenda.hasValue()) {
      this.citas.loadAgenda(this.citas.fechaAgenda());
    }
    const seleccionada = this.citas.selected.value();
    if (seleccionada && notificacion.enlace === `/citas/${seleccionada.id}`) {
      this.citas.loadOne(seleccionada.id);
    }
  }
}
