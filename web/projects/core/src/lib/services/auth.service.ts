import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, shareReplay, tap, throwError } from 'rxjs';

import {
  AuthResponse,
  LoginRequest,
  LoginResult,
  ROLES,
  RegisterRequest,
  Rol,
  Session,
  SessionUser,
} from '../models';
import { jwtExpirationMs } from '../utils/jwt';
import { AuthApiService } from './api/auth-api.service';
import { NotificationService } from './notification.service';
import { StorageService } from './storage.service';

export const SESSION_STORAGE_KEY = 'tusalud.session';
export const LOGIN_URL = '/auth/login';

export type LogoutReason = 'manual' | 'expired' | 'unauthorized';

const MAX_TIMER_MS = 2_147_483_647;
const ANTICIPO_RENOVACION_MS = 60_000;

const HOME_BY_ROLE: readonly (readonly [Rol, string])[] = [
  ['ROLE_ADMIN', '/admin/panel'],
  ['ROLE_MEDICO', '/agenda'],
  ['ROLE_PACIENTE', '/citas'],
  ['ROLE_LABORATORIO', '/laboratorio'],
  ['ROLE_FARMACIA', '/farmacia'],
  ['ROLE_ALMACEN', '/almacen'],
  ['ROLE_RECEPCION', '/recepcion'],
  ['ROLE_CAJA', '/caja'],
  ['ROLE_ENFERMERIA', '/hospitalizacion'],
];

const DEFAULT_HOME = '/perfil';

function isRol(value: unknown): value is Rol {
  return typeof value === 'string' && (ROLES as readonly string[]).includes(value);
}

function isSession(value: unknown): value is Session {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const session = value as Session;
  const user = session.user as SessionUser | undefined;
  return (
    typeof session.token === 'string' &&
    typeof session.expiresAt === 'number' &&
    (session.refreshToken === undefined || typeof session.refreshToken === 'string') &&
    (session.refreshExpiresAt === undefined || typeof session.refreshExpiresAt === 'number') &&
    typeof user === 'object' &&
    user !== null &&
    typeof user.id === 'string' &&
    typeof user.email === 'string' &&
    typeof user.nombreCompleto === 'string' &&
    Array.isArray(user.roles) &&
    user.roles.every(isRol)
  );
}

export function toSession(response: AuthResponse, now = Date.now()): Session {
  return {
    token: response.token,
    expiresAt: jwtExpirationMs(response.token) ?? now + response.expiresIn,
    ...(response.refreshToken
      ? {
          refreshToken: response.refreshToken,
          refreshExpiresAt: now + (response.refreshExpiresIn ?? 0),
        }
      : {}),
    user: {
      id: response.usuarioId,
      email: response.email,
      nombreCompleto: response.nombreCompleto,
      roles: response.roles.filter(isRol),
    },
  };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApiService);
  private readonly storage = inject(StorageService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  private readonly state = signal<Session | null>(this.restoreSession());
  private renovando: Observable<string> | null = null;

  readonly session = this.state.asReadonly();
  readonly isAuthenticated = computed(() => this.state() !== null);
  readonly user = computed(() => this.state()?.user ?? null);
  readonly token = computed(() => this.state()?.token ?? null);
  readonly roles = computed<readonly Rol[]>(() => this.state()?.user.roles ?? []);
  readonly isAdmin = computed(() => this.roles().includes('ROLE_ADMIN'));
  readonly isMedico = computed(() => this.roles().includes('ROLE_MEDICO'));
  readonly isPaciente = computed(() => this.roles().includes('ROLE_PACIENTE'));
  readonly isLaboratorio = computed(() => this.roles().includes('ROLE_LABORATORIO'));
  readonly isFarmacia = computed(() => this.roles().includes('ROLE_FARMACIA'));
  readonly isAlmacen = computed(() => this.roles().includes('ROLE_ALMACEN'));
  readonly isRecepcion = computed(() => this.roles().includes('ROLE_RECEPCION'));
  readonly isCaja = computed(() => this.roles().includes('ROLE_CAJA'));
  readonly isEnfermeria = computed(() => this.roles().includes('ROLE_ENFERMERIA'));
  readonly homeUrl = computed(() => {
    const roles = this.roles();
    return HOME_BY_ROLE.find(([rol]) => roles.includes(rol))?.[1] ?? DEFAULT_HOME;
  });

  constructor() {
    effect((onCleanup) => {
      const session = this.state();
      if (!session) {
        this.storage.remove(SESSION_STORAGE_KEY);
        return;
      }
      this.storage.write(SESSION_STORAGE_KEY, session);
      const renovable = this.puedeRenovar(session);
      const vence = renovable ? session.refreshExpiresAt! : session.expiresAt;
      const espera = renovable
        ? Math.max(session.expiresAt - Date.now() - ANTICIPO_RENOVACION_MS, 1000)
        : vence - Date.now();
      const timer = setTimeout(
        () => {
          if (renovable) {
            this.refresh().subscribe({ error: () => undefined });
          } else {
            this.logout('expired');
          }
        },
        Math.min(Math.max(espera, 0), MAX_TIMER_MS),
      );
      onCleanup(() => clearTimeout(timer));
    });
  }

  login(request: LoginRequest): Observable<LoginResult> {
    return this.api
      .login({ ...request, email: request.email.trim().toLowerCase() })
      .pipe(
        map((response) =>
          response.mfaRequerido && response.mfaToken
            ? ({ mfa: { mfaToken: response.mfaToken } } satisfies LoginResult)
            : this.abrirSesion(response as AuthResponse),
        ),
      );
  }

  puedeRenovar(session: Session | null = this.state()): boolean {
    return !!session?.refreshToken && (session.refreshExpiresAt ?? 0) > Date.now();
  }

  refresh(): Observable<string> {
    const session = this.state();
    if (!session?.refreshToken || !this.puedeRenovar(session)) {
      return throwError(() => new Error('La sesión no se puede renovar'));
    }
    if (!this.renovando) {
      this.renovando = this.api.refresh({ refreshToken: session.refreshToken }).pipe(
        map((response) => toSession(response)),
        tap((nueva) => this.state.set(nueva)),
        map((nueva) => nueva.token),
        catchError((error: unknown) => {
          this.logout('expired');
          return throwError(() => error);
        }),
        finalize(() => (this.renovando = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.renovando;
  }

  verificarMfa(mfaToken: string, codigo: string): Observable<SessionUser> {
    return this.api.verificarMfa({ mfaToken, codigo }).pipe(
      map((response) => toSession(response)),
      tap((session) => this.state.set(session)),
      map((session) => session.user),
    );
  }

  register(request: RegisterRequest): Observable<SessionUser> {
    return this.api.register({ ...request, email: request.email.trim().toLowerCase() }).pipe(
      map((response) => toSession(response)),
      tap((session) => this.state.set(session)),
      map((session) => session.user),
    );
  }

  updateNombreCompleto(nombreCompleto: string): void {
    this.state.update((session) =>
      session ? { ...session, user: { ...session.user, nombreCompleto } } : session,
    );
  }

  hasAnyRole(roles: readonly Rol[]): boolean {
    const current = this.roles();
    return roles.some((rol) => current.includes(rol));
  }

  logout(reason: LogoutReason = 'manual'): void {
    const wasAuthenticated = this.state() !== null;
    if (wasAuthenticated && reason === 'manual') {
      this.api.logout().subscribe({ error: () => undefined });
    }
    this.state.set(null);
    if (!wasAuthenticated) {
      return;
    }
    if (reason === 'manual') {
      void this.router.navigateByUrl(LOGIN_URL);
      return;
    }
    this.notifications.warning(
      reason === 'expired'
        ? 'Tu sesión expiró. Inicia sesión nuevamente.'
        : 'Tu sesión ya no es válida. Inicia sesión nuevamente.',
    );
    const returnUrl = this.router.url;
    void this.router.navigate([LOGIN_URL], {
      queryParams: returnUrl && !returnUrl.startsWith('/auth') ? { returnUrl } : undefined,
    });
  }

  private abrirSesion(response: AuthResponse): LoginResult {
    const session = toSession(response);
    this.state.set(session);
    return { sesion: session.user };
  }

  private restoreSession(): Session | null {
    const session = this.storage.read(SESSION_STORAGE_KEY, isSession);
    if (session && session.expiresAt > Date.now()) {
      return session;
    }
    this.storage.remove(SESSION_STORAGE_KEY);
    return null;
  }
}
