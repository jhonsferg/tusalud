export type MensajeEntrante = { t: string; [clave: string]: unknown };

export interface ManejadoresCanal {
  alAbrir: () => void;
  alMensaje: (mensaje: MensajeEntrante) => void;
  alCerrar: (codigo: number) => void;
}

export class CanalSenalizacion {
  private socket: WebSocket | null = null;
  private latido: ReturnType<typeof setInterval> | null = null;

  constructor(
    private readonly url: string,
    private readonly manejadores: ManejadoresCanal,
  ) {}

  abrir(): void {
    const socket = new WebSocket(this.url);
    this.socket = socket;
    socket.onopen = () => {
      this.latido = setInterval(() => this.enviar({ t: 'ping' }), 25_000);
      this.manejadores.alAbrir();
    };
    socket.onmessage = (evento) => {
      try {
        const mensaje = JSON.parse(String(evento.data)) as MensajeEntrante;
        if (typeof mensaje.t === 'string') {
          this.manejadores.alMensaje(mensaje);
        }
      } catch {
        return;
      }
    };
    socket.onclose = (evento) => {
      this.detener();
      this.manejadores.alCerrar(evento.code);
    };
  }

  enviar(mensaje: Record<string, unknown>): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(mensaje));
    }
  }

  cerrar(): void {
    this.detener();
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.close(1000);
      this.socket = null;
    }
  }

  private detener(): void {
    if (this.latido) {
      clearInterval(this.latido);
      this.latido = null;
    }
  }
}

export function urlDeSenalizacion(
  ruta: string,
  ticket: string,
  ubicacion: Location = location,
): string {
  const protocolo = ubicacion.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocolo}://${ubicacion.host}${ruta}?ticket=${encodeURIComponent(ticket)}`;
}
