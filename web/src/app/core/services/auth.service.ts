import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import {
  AuthResponse,
  LoginRequest,
  ROLES,
  RegisterRequest,
  Rol,
  Session,
  SessionUser,
} from '@tusalud/app/core/models';
import { jwtExpirationMs } from '@tusalud/app/core/utils/jwt';
import { AuthApiService } from '@tusalud/app/core/services/api/auth-api.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { StorageService } from '@tusalud/app/core/services/storage.service';

export const SESSION_STORAGE_KEY = 'tusalud.session';
export const LOGIN_URL = '/auth/login';

export type LogoutReason = 'manual' | 'expired' | 'unauthorized';

const MAX_TIMER_MS = 2_147_483_647;

const HOME_BY_ROLE: readonly (readonly [Rol, string])[] = [
  ['ROLE_ADMIN', '/admin/auditoria'],
  ['ROLE_MEDICO', '/agenda'],
  ['ROLE_PACIENTE', '/citas'],
];

const DEFAULT_HOME = '/especialidades';

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
    typeof user === 'object' &&
    user !== null &&
    typeof user.id === 'number' &&
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

  readonly session = this.state.asReadonly();
  readonly isAuthenticated = computed(() => this.state() !== null);
  readonly user = computed(() => this.state()?.user ?? null);
  readonly token = computed(() => this.state()?.token ?? null);
  readonly roles = computed<readonly Rol[]>(() => this.state()?.user.roles ?? []);
  readonly isAdmin = computed(() => this.roles().includes('ROLE_ADMIN'));
  readonly isMedico = computed(() => this.roles().includes('ROLE_MEDICO'));
  readonly isPaciente = computed(() => this.roles().includes('ROLE_PACIENTE'));
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
      const remaining = Math.min(session.expiresAt - Date.now(), MAX_TIMER_MS);
      const timer = setTimeout(() => this.logout('expired'), Math.max(remaining, 0));
      onCleanup(() => clearTimeout(timer));
    });
  }

  login(request: LoginRequest): Observable<SessionUser> {
    return this.api.login({ ...request, email: request.email.trim().toLowerCase() }).pipe(
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

  hasAnyRole(roles: readonly Rol[]): boolean {
    const current = this.roles();
    return roles.some((rol) => current.includes(rol));
  }

  logout(reason: LogoutReason = 'manual'): void {
    const wasAuthenticated = this.state() !== null;
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

  private restoreSession(): Session | null {
    const session = this.storage.read(SESSION_STORAGE_KEY, isSession);
    if (session && session.expiresAt > Date.now()) {
      return session;
    }
    this.storage.remove(SESSION_STORAGE_KEY);
    return null;
  }
}
