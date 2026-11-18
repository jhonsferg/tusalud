import {
  DOCUMENT,
  EnvironmentProviders,
  Injectable,
  InjectionToken,
  Provider,
  inject,
  makeEnvironmentProviders,
  signal,
} from '@angular/core';

import { AuthService } from './auth.service';

export interface InactividadConfig {
  readonly bloqueoMinutos: number;
  readonly avisoSegundos: number;
  readonly sesionMaximaHoras: number;
}

export const INACTIVIDAD_PREDETERMINADA: InactividadConfig = {
  bloqueoMinutos: 10,
  avisoSegundos: 60,
  sesionMaximaHoras: 12,
};

export const INACTIVIDAD_CONFIG = new InjectionToken<InactividadConfig>('INACTIVIDAD_CONFIG', {
  providedIn: 'root',
  factory: () => INACTIVIDAD_PREDETERMINADA,
});

export function provideInactividad(config: Partial<InactividadConfig>): EnvironmentProviders {
  const proveedor: Provider = {
    provide: INACTIVIDAD_CONFIG,
    useValue: { ...INACTIVIDAD_PREDETERMINADA, ...config },
  };
  return makeEnvironmentProviders([proveedor]);
}

export type EstadoInactividad = 'activo' | 'aviso' | 'bloqueado';

export const CLAVE_ACTIVIDAD = 'tusalud.actividad';
export const CLAVE_BLOQUEO = 'tusalud.bloqueo';
export const CLAVE_INICIO_SESION = 'tusalud.sesion.inicio';

const EVENTOS_ACTIVIDAD = ['pointerdown', 'keydown', 'wheel', 'touchstart'] as const;

@Injectable({ providedIn: 'root' })
export class InactividadService {
  private readonly auth = inject(AuthService);
  private readonly documento = inject(DOCUMENT);
  private readonly config = inject(INACTIVIDAD_CONFIG);

  private readonly estadoState = signal<EstadoInactividad>('activo');
  private readonly restantesState = signal(0);
  private temporizador: ReturnType<typeof setInterval> | null = null;
  private ultimaLocal = Date.now();
  private escuchando = false;

  readonly estado = this.estadoState.asReadonly();
  readonly segundosRestantes = this.restantesState.asReadonly();

  iniciar(): void {
    if (this.escuchando) {
      return;
    }
    this.escuchando = true;
    this.ultimaLocal = Date.now();
    if (this.leer(CLAVE_INICIO_SESION) === null) {
      this.escribir(CLAVE_INICIO_SESION, String(Date.now()));
    }
    const ventana = this.documento.defaultView;
    for (const evento of EVENTOS_ACTIVIDAD) {
      this.documento.addEventListener(evento, this.alActividad, { passive: true });
    }
    ventana?.addEventListener('storage', this.alAlmacenamiento);
    this.temporizador = setInterval(() => this.verificar(), 1000);
    this.verificar();
  }

  detener(): void {
    if (!this.escuchando) {
      return;
    }
    this.escuchando = false;
    for (const evento of EVENTOS_ACTIVIDAD) {
      this.documento.removeEventListener(evento, this.alActividad);
    }
    this.documento.defaultView?.removeEventListener('storage', this.alAlmacenamiento);
    if (this.temporizador) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
    this.estadoState.set('activo');
    if (!this.auth.isAuthenticated()) {
      this.quitar(CLAVE_INICIO_SESION);
      this.quitar(CLAVE_BLOQUEO);
      this.quitar(CLAVE_ACTIVIDAD);
    }
  }

  seguirTrabajando(): void {
    this.registrarActividad(Date.now());
    this.estadoState.set('activo');
  }

  bloquearAhora(): void {
    this.escribir(CLAVE_BLOQUEO, '1');
    this.estadoState.set('bloqueado');
  }

  desbloquear(): void {
    this.escribir(CLAVE_BLOQUEO, '0');
    this.registrarActividad(Date.now());
    this.estadoState.set('activo');
  }

  cerrarSesion(): void {
    this.limpiar();
    this.auth.logout('manual');
  }

  verificar(ahora = Date.now()): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    const inicio = Number(this.leer(CLAVE_INICIO_SESION) ?? ahora);
    if (ahora - inicio >= this.config.sesionMaximaHoras * 3_600_000) {
      this.limpiar();
      this.auth.logout('expired');
      return;
    }
    if (this.leer(CLAVE_BLOQUEO) === '1') {
      this.estadoState.set('bloqueado');
      return;
    }
    const ultima = Math.max(this.ultimaLocal, Number(this.leer(CLAVE_ACTIVIDAD) ?? 0));
    const inactivo = ahora - ultima;
    const limite = this.config.bloqueoMinutos * 60_000;
    if (inactivo >= limite) {
      this.bloquearAhora();
      return;
    }
    const restante = limite - inactivo;
    if (restante <= this.config.avisoSegundos * 1000) {
      this.restantesState.set(Math.ceil(restante / 1000));
      this.estadoState.set('aviso');
      return;
    }
    if (this.estadoState() !== 'activo') {
      this.estadoState.set('activo');
    }
  }

  registrarActividad(ahora = Date.now()): void {
    this.ultimaLocal = ahora;
    this.escribir(CLAVE_ACTIVIDAD, String(ahora));
  }

  private limpiar(): void {
    this.detener();
    this.quitar(CLAVE_INICIO_SESION);
    this.quitar(CLAVE_BLOQUEO);
    this.quitar(CLAVE_ACTIVIDAD);
  }

  private readonly alActividad = (): void => {
    if (this.estadoState() === 'bloqueado') {
      return;
    }
    const ahora = Date.now();
    if (ahora - this.ultimaLocal > 1000) {
      this.registrarActividad(ahora);
      if (this.estadoState() === 'aviso') {
        this.estadoState.set('activo');
      }
    }
  };

  private readonly alAlmacenamiento = (evento: StorageEvent): void => {
    if (evento.key === CLAVE_BLOQUEO || evento.key === CLAVE_ACTIVIDAD) {
      this.verificar();
    }
  };

  private leer(clave: string): string | null {
    try {
      return this.documento.defaultView?.localStorage.getItem(clave) ?? null;
    } catch {
      return null;
    }
  }

  private escribir(clave: string, valor: string): void {
    try {
      this.documento.defaultView?.localStorage.setItem(clave, valor);
    } catch {
      return;
    }
  }

  private quitar(clave: string): void {
    try {
      this.documento.defaultView?.localStorage.removeItem(clave);
    } catch {
      return;
    }
  }
}
