import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';

import { API_URL } from '../config/api.config';
import { AuthService } from './auth.service';

export interface RealtimeEvent {
  readonly event: string;
  readonly data: string;
}

const MIN_BACKOFF_MS = 1_000;
const MAX_BACKOFF_MS = 30_000;
const SILENCE_LIMIT_MS = 75_000;

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private readonly auth = inject(AuthService);
  private readonly streamUrl = `${inject(API_URL)}/notificaciones/stream`;
  private readonly connectedState = signal(false);
  private readonly eventSubject = new Subject<RealtimeEvent>();
  private controller: AbortController | null = null;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private attempt = 0;

  readonly connected = this.connectedState.asReadonly();
  readonly events$ = this.eventSubject.asObservable();

  constructor() {
    inject(DestroyRef).onDestroy(() => this.disconnect());
  }

  connect(): void {
    if (this.controller) {
      return;
    }
    void this.open();
  }

  disconnect(): void {
    if (this.retryTimer) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
    this.controller?.abort();
    this.controller = null;
    this.attempt = 0;
    this.connectedState.set(false);
  }

  private async open(): Promise<void> {
    const token = this.auth.token();
    if (!token) {
      return;
    }
    const controller = new AbortController();
    this.controller = controller;
    let watchdog: ReturnType<typeof setTimeout> | null = null;
    const rearm = (): void => {
      if (watchdog) {
        clearTimeout(watchdog);
      }
      watchdog = setTimeout(() => controller.abort(), SILENCE_LIMIT_MS);
    };

    try {
      const response = await fetch(this.streamUrl, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal: controller.signal,
        cache: 'no-store',
      });
      if (response.status === 401) {
        this.controller = null;
        if (this.auth.puedeRenovar()) {
          this.auth.refresh().subscribe({ next: () => void this.open(), error: () => undefined });
        } else {
          this.auth.logout('unauthorized');
        }
        return;
      }
      if (!response.ok || !response.body) {
        throw new Error(`Flujo de notificaciones no disponible: ${response.status}`);
      }
      this.attempt = 0;
      this.connectedState.set(true);
      rearm();

      const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
      let buffer = '';
      for (;;) {
        const { done, value } = await reader.read();
        if (done) {
          break;
        }
        rearm();
        buffer += value.replace(/\r\n/g, '\n');
        let corte = buffer.indexOf('\n\n');
        while (corte >= 0) {
          this.procesarBloque(buffer.slice(0, corte));
          buffer = buffer.slice(corte + 2);
          corte = buffer.indexOf('\n\n');
        }
      }
    } catch {
      this.connectedState.set(false);
    } finally {
      if (watchdog) {
        clearTimeout(watchdog);
      }
    }

    this.connectedState.set(false);
    if (this.controller === controller) {
      this.controller = null;
      this.programarReconexion();
    }
  }

  private procesarBloque(bloque: string): void {
    let event = 'message';
    const datos: string[] = [];
    for (const linea of bloque.split('\n')) {
      if (linea.startsWith(':') || linea === '') {
        continue;
      }
      if (linea.startsWith('event:')) {
        event = linea.slice(6).trim();
      } else if (linea.startsWith('data:')) {
        datos.push(linea.slice(5).replace(/^ /, ''));
      }
    }
    if (datos.length > 0 || event !== 'message') {
      this.eventSubject.next({ event, data: datos.join('\n') });
    }
  }

  private programarReconexion(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    const espera = Math.min(MIN_BACKOFF_MS * 2 ** this.attempt, MAX_BACKOFF_MS);
    this.attempt++;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.connect();
    }, espera);
  }
}
