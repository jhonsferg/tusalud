import { DestroyRef, Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, Subscription, tap, timer } from 'rxjs';

import { NotificacionResponse } from '@tusalud/app/core/models';
import { NotificacionesApiService } from '@tusalud/app/core/services/api/notificaciones-api.service';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

const POLL_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class NotificacionesStore {
  private readonly api = inject(NotificacionesApiService);
  private readonly auth = inject(AuthService);
  private readonly noLeidasState = signal(0);
  private polling: Subscription | null = null;

  readonly lista = new AsyncState<NotificacionResponse[]>();
  readonly noLeidas = this.noLeidasState.asReadonly();
  readonly items = computed(() => this.lista.value() ?? []);
  readonly recientes = computed(() => this.items().slice(0, 6));

  constructor() {
    resetOnLogout(() => this.reset());
    effect(() => {
      const autenticado = this.auth.isAuthenticated();
      untracked(() => (autenticado ? this.iniciarSondeo() : this.detenerSondeo()));
    });
    inject(DestroyRef).onDestroy(() => this.detenerSondeo());
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

  private iniciarSondeo(): void {
    this.detenerSondeo();
    this.polling = timer(0, POLL_MS).subscribe(() => this.refrescarContador());
  }

  private detenerSondeo(): void {
    this.polling?.unsubscribe();
    this.polling = null;
  }
}
