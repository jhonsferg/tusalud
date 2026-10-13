import { TestBed } from '@angular/core/testing';

import { LoadingService } from './loading.service';
import { NotificationService } from './notification.service';
import { StorageService } from './storage.service';

describe('LoadingService', () => {
  it('cuenta peticiones pendientes sin bajar de cero', () => {
    const loading = TestBed.inject(LoadingService);
    loading.start();
    loading.start();
    expect(loading.isLoading()).toBe(true);
    loading.stop();
    loading.stop();
    loading.stop();
    expect(loading.pendingRequests()).toBe(0);
    expect(loading.isLoading()).toBe(false);
  });
});

describe('NotificationService', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('muestra notificaciones y las descarta automáticamente', () => {
    const notifications = TestBed.inject(NotificationService);
    const id = notifications.error('Algo falló', 1000);
    notifications.success('Persistente', 0);

    expect(notifications.notifications().map((n) => n.type)).toEqual(['error', 'success']);
    vi.advanceTimersByTime(1000);
    expect(notifications.notifications().map((n) => n.message)).toEqual(['Persistente']);
    expect(notifications.notifications().some((n) => n.id === id)).toBe(false);
  });

  it('permite descartar y limpiar manualmente', () => {
    const notifications = TestBed.inject(NotificationService);
    const id = notifications.info('Uno');
    notifications.warning('Dos');
    notifications.dismiss(id);
    expect(notifications.notifications().length).toBe(1);
    notifications.clear();
    expect(notifications.hasNotifications()).toBe(false);
  });
});

describe('StorageService', () => {
  const isNumberList = (value: unknown): value is number[] =>
    Array.isArray(value) && value.every((item) => typeof item === 'number');

  beforeEach(() => localStorage.clear());

  it('lee y escribe JSON validando su forma', () => {
    const storage = TestBed.inject(StorageService);
    storage.write('clave', [1, 2]);
    expect(storage.read('clave', isNumberList)).toEqual([1, 2]);
  });

  it('descarta valores corruptos o con forma inválida', () => {
    const storage = TestBed.inject(StorageService);
    localStorage.setItem('roto', '{no json');
    localStorage.setItem('invalido', JSON.stringify(['a']));
    expect(storage.read('roto', isNumberList)).toBeNull();
    expect(storage.read('invalido', isNumberList)).toBeNull();
    expect(localStorage.getItem('roto')).toBeNull();
    expect(localStorage.getItem('invalido')).toBeNull();
  });
});
