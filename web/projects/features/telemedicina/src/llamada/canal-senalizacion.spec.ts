import { CanalSenalizacion, MensajeEntrante } from './canal-senalizacion';

class SocketFalso {
  static ultimo: SocketFalso;
  static readonly OPEN = 1;
  readyState = 0;
  enviados: string[] = [];
  cerradoCon: number | null = null;
  onopen: (() => void) | null = null;
  onmessage: ((evento: { data: string }) => void) | null = null;
  onclose: ((evento: { code: number }) => void) | null = null;

  constructor(readonly url: string) {
    SocketFalso.ultimo = this;
  }

  send(dato: string): void {
    this.enviados.push(dato);
  }

  close(codigo: number): void {
    this.cerradoCon = codigo;
  }

  abrir(): void {
    this.readyState = 1;
    this.onopen?.();
  }
}

describe('CanalSenalizacion', () => {
  const original = globalThis.WebSocket;

  beforeEach(() => {
    vi.useFakeTimers();
    (globalThis as unknown as { WebSocket: unknown }).WebSocket = SocketFalso;
  });

  afterEach(() => {
    vi.useRealTimers();
    (globalThis as unknown as { WebSocket: unknown }).WebSocket = original;
  });

  function crear() {
    const mensajes: MensajeEntrante[] = [];
    const alAbrir = vi.fn();
    const alCerrar = vi.fn();
    const canal = new CanalSenalizacion('ws://x/ws/teleconsulta?ticket=t', {
      alAbrir,
      alMensaje: (m) => mensajes.push(m),
      alCerrar,
    });
    canal.abrir();
    return { canal, mensajes, alAbrir, alCerrar };
  }

  it('solo envía cuando el canal está abierto y serializa el mensaje', () => {
    const { canal, alAbrir } = crear();
    canal.enviar({ t: 'chat', texto: 'antes' });
    expect(SocketFalso.ultimo.enviados).toEqual([]);
    SocketFalso.ultimo.abrir();
    expect(alAbrir).toHaveBeenCalled();
    canal.enviar({ t: 'chat', texto: 'hola' });
    expect(SocketFalso.ultimo.enviados).toEqual(['{"t":"chat","texto":"hola"}']);
  });

  it('entrega los mensajes válidos e ignora los malformados', () => {
    const { mensajes } = crear();
    SocketFalso.ultimo.abrir();
    SocketFalso.ultimo.onmessage?.({ data: '{"t":"estado","admitido":true}' });
    SocketFalso.ultimo.onmessage?.({ data: 'no es json' });
    SocketFalso.ultimo.onmessage?.({ data: '{"sin":"tipo"}' });
    expect(mensajes).toEqual([{ t: 'estado', admitido: true }]);
  });

  it('envía un latido cada 25 segundos y lo detiene al cerrar', () => {
    const { canal } = crear();
    SocketFalso.ultimo.abrir();
    vi.advanceTimersByTime(25_000);
    expect(SocketFalso.ultimo.enviados).toEqual(['{"t":"ping"}']);
    canal.cerrar();
    vi.advanceTimersByTime(60_000);
    expect(SocketFalso.ultimo.enviados).toHaveLength(1);
    expect(SocketFalso.ultimo.cerradoCon).toBe(1000);
  });

  it('avisa el código de cierre cuando el servidor corta la conexión', () => {
    const { alCerrar } = crear();
    SocketFalso.ultimo.abrir();
    SocketFalso.ultimo.onclose?.({ code: 1008 });
    expect(alCerrar).toHaveBeenCalledWith(1008);
  });
});
