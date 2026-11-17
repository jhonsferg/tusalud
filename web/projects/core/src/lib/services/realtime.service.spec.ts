import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { createSession } from '../../testing/auth-fixtures';
import { SESSION_STORAGE_KEY } from './auth.service';
import { RealtimeEvent, RealtimeService } from './realtime.service';

function flujo(bloques: string[], cerrar: boolean): Response {
  const codificador = new TextEncoder();
  const body = new ReadableStream<Uint8Array>({
    start(controlador) {
      bloques.forEach((bloque) => controlador.enqueue(codificador.encode(bloque)));
      if (cerrar) {
        controlador.close();
      }
    },
  });
  return new Response(body, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
}

async function esperar(condicion: () => boolean): Promise<void> {
  for (let i = 0; i < 100 && !condicion(); i++) {
    await new Promise((resolver) => setTimeout(resolver, 5));
  }
}

describe('RealtimeService', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_PACIENTE'])));
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => {
    TestBed.inject(RealtimeService).disconnect();
    vi.restoreAllMocks();
  });

  it('abre el flujo con el token, interpreta los eventos y marca la conexión', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(
        flujo(
          [
            'event: conectado\ndata: ok\n\n',
            ': latido\n\n',
            'event: notificacion\ndata: {"id":7,"titulo":"Cita confirmada"}\n\nevent: ping\ndata: 1\n\n',
          ],
          false,
        ),
      );
    const servicio = TestBed.inject(RealtimeService);
    const recibidos: RealtimeEvent[] = [];
    servicio.events$.subscribe((evento) => recibidos.push(evento));

    servicio.connect();
    await esperar(() => recibidos.length >= 3);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, opciones] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/notificaciones/stream');
    expect((opciones?.headers as Record<string, string>)['Authorization']).toMatch(/^Bearer /);
    expect(recibidos.map((evento) => evento.event)).toEqual(['conectado', 'notificacion', 'ping']);
    expect(JSON.parse(recibidos[1].data)).toEqual({ id: 7, titulo: 'Cita confirmada' });
    expect(servicio.connected()).toBe(true);

    servicio.disconnect();
    expect(servicio.connected()).toBe(false);
  });

  it('reconecta con espera cuando el servidor cierra el flujo', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(() => Promise.resolve(flujo([], true)));
    const servicio = TestBed.inject(RealtimeService);

    servicio.connect();
    await vi.advanceTimersByTimeAsync(10);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(servicio.connected()).toBe(false);

    await vi.advanceTimersByTimeAsync(1_100);
    expect(fetchMock.mock.calls.length).toBeGreaterThanOrEqual(2);
    vi.useRealTimers();
  });
});
