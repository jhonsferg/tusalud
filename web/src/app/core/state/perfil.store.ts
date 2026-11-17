import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  PasswordChangeRequest,
  PerfilResponse,
  PerfilUpdateRequest,
  PreferenciasResponse,
  SesionResponse,
} from '@tusalud/app/core/models';
import { PerfilApiService } from '@tusalud/app/core/services/api/perfil-api.service';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class PerfilStore {
  private readonly api = inject(PerfilApiService);
  private readonly auth = inject(AuthService);

  readonly perfil = new AsyncState<PerfilResponse>();
  readonly preferencias = new AsyncState<PreferenciasResponse>();
  readonly sesiones = new AsyncState<SesionResponse[]>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  load(): void {
    this.perfil.load(this.api.obtener());
  }

  actualizar(request: PerfilUpdateRequest): Observable<PerfilResponse> {
    return this.api.actualizar(request).pipe(tap((perfil) => this.aplicar(perfil)));
  }

  actualizarAvatar(imagen: string): Observable<PerfilResponse> {
    return this.api.actualizarAvatar(imagen).pipe(tap((perfil) => this.aplicar(perfil)));
  }

  eliminarAvatar(): Observable<void> {
    return this.api
      .eliminarAvatar()
      .pipe(
        tap(() => this.perfil.update((perfil) => (perfil ? { ...perfil, avatar: null } : perfil))),
      );
  }

  cambiarPassword(request: PasswordChangeRequest): Observable<void> {
    return this.api.cambiarPassword(request);
  }

  cargarPreferencias(): void {
    this.preferencias.load(this.api.obtenerPreferencias());
  }

  guardarPreferencias(request: PreferenciasResponse): Observable<PreferenciasResponse> {
    return this.api
      .actualizarPreferencias(request)
      .pipe(tap((preferencias) => this.preferencias.set(preferencias)));
  }

  cargarSesiones(): void {
    this.sesiones.load(this.api.listarSesiones());
  }

  cerrarSesion(id: number): Observable<void> {
    return this.api
      .cerrarSesion(id)
      .pipe(tap(() => this.sesiones.update((items) => items?.filter((item) => item.id !== id))));
  }

  cerrarOtrasSesiones(): Observable<void> {
    return this.api
      .cerrarOtrasSesiones()
      .pipe(tap(() => this.sesiones.update((items) => items?.filter((item) => item.actual))));
  }

  reset(): void {
    this.perfil.reset();
    this.preferencias.reset();
    this.sesiones.reset();
  }

  private aplicar(perfil: PerfilResponse): void {
    this.perfil.set(perfil);
    this.auth.updateNombreCompleto(perfil.nombreCompleto);
  }
}
