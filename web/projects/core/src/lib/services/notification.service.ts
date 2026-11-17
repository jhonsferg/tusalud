import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';

export type NotificationType = 'success' | 'info' | 'warning' | 'error';

export interface AppNotification {
  readonly id: number;
  readonly type: NotificationType;
  readonly message: string;
}

const DEFAULT_DURATION_MS = 5000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly items = signal<readonly AppNotification[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private nextId = 1;

  readonly notifications = this.items.asReadonly();
  readonly hasNotifications = computed(() => this.items().length > 0);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.clear());
  }

  show(type: NotificationType, message: string, durationMs = DEFAULT_DURATION_MS): number {
    const id = this.nextId++;
    this.items.update((items) => [...items, { id, type, message }]);
    if (durationMs > 0) {
      this.timers.set(
        id,
        setTimeout(() => this.dismiss(id), durationMs),
      );
    }
    return id;
  }

  success(message: string, durationMs?: number): number {
    return this.show('success', message, durationMs);
  }

  info(message: string, durationMs?: number): number {
    return this.show('info', message, durationMs);
  }

  warning(message: string, durationMs?: number): number {
    return this.show('warning', message, durationMs);
  }

  error(message: string, durationMs?: number): number {
    return this.show('error', message, durationMs);
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this.items.update((items) => items.filter((item) => item.id !== id));
  }

  clear(): void {
    this.timers.forEach((timer) => clearTimeout(timer));
    this.timers.clear();
    this.items.set([]);
  }
}
