import { Injectable, effect, inject, signal, untracked } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  PasswordChangeRequest,
  PerfilResponse,
  PerfilUpdateRequest,
  PreferenciasResponse,
  SesionResponse,
} from '../models';
import { PerfilApiService } from '../services/api/perfil-api.service';
import { ArchivoBlob, ArchivoBlobService } from '../services/archivo-blob.service';
import { AuthService } from '../services/auth.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class PerfilStore {
  private readonly api = inject(PerfilApiService);
  private readonly auth = inject(AuthService);
  private readonly blobs = inject(ArchivoBlobService);
  private avatarActual: ArchivoBlob | null = null;
  private avatarCargado: string | null = null;

  readonly perfil = new AsyncState<PerfilResponse>();
  readonly preferencias = new AsyncState<PreferenciasResponse>();
  readonly sesiones = new AsyncState<SesionResponse[]>();

  readonly avatarUrl = signal<string | null>(null);

  constructor() {
    resetOnLogout(() => this.reset());
    effect(() => {
      const id = this.perfil.value()?.avatarId ?? null;
      untracked(() => this.sincronizarAvatar(id));
    });
  }

  load(): void {
    this.perfil.load(this.api.obtener());
  }

  actualizar(request: PerfilUpdateRequest): Observable<PerfilResponse> {
    return this.api.actualizar(request).pipe(tap((perfil) => this.aplicar(perfil)));
  }

  actualizarAvatar(archivo: File): Observable<PerfilResponse> {
    return this.api.actualizarAvatar(archivo).pipe(tap((perfil) => this.aplicar(perfil)));
  }

  eliminarAvatar(): Observable<void> {
    return this.api
      .eliminarAvatar()
      .pipe(
        tap(() =>
          this.perfil.update((perfil) => (perfil ? { ...perfil, avatarId: null } : perfil)),
        ),
      );
  }

  cambiarPassword(request: PasswordChangeRequest): Observable<void> {
    return this.api.cambiarPassword(request);
  }

  cargarPreferencias(): void {
    this.preferencias.load(this.api.obtenerPreferencias());
  }

  guardarPreferencias(
    request: Omit<PreferenciasResponse, 'tourCompletado'>,
  ): Observable<PreferenciasResponse> {
    const tourCompletado = this.preferencias.value()?.tourCompletado ?? false;
    return this.api
      .actualizarPreferencias({ ...request, tourCompletado })
      .pipe(tap((preferencias) => this.preferencias.set(preferencias)));
  }

  marcarTour(completado: boolean): Observable<PreferenciasResponse> {
    const actual = this.preferencias.value();
    return this.api
      .actualizarPreferencias({
        recordatoriosActivos: actual?.recordatoriosActivos ?? true,
        minutosAntelacionRecordatorio: actual?.minutosAntelacionRecordatorio ?? 1440,
        alertasInicioSesion: actual?.alertasInicioSesion ?? true,
        tourCompletado: completado,
      })
      .pipe(tap((preferencias) => this.preferencias.set(preferencias)));
  }

  cargarSesiones(): void {
    this.sesiones.load(this.api.listarSesiones());
  }

  cerrarSesion(id: string): Observable<void> {
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
    this.liberarAvatar();
    this.perfil.reset();
    this.preferencias.reset();
    this.sesiones.reset();
  }

  private sincronizarAvatar(id: string | null): void {
    if (id === this.avatarCargado) {
      return;
    }
    this.liberarAvatar();
    this.avatarCargado = id;
    if (id === null) {
      return;
    }
    this.blobs.cargar(id).subscribe({
      next: (blob) => {
        if (this.avatarCargado !== id) {
          blob.liberar();
          return;
        }
        this.avatarActual = blob;
        this.avatarUrl.set(blob.url);
      },
      error: () => {
        if (this.avatarCargado === id) {
          this.avatarUrl.set(null);
        }
      },
    });
  }

  private liberarAvatar(): void {
    this.avatarActual?.liberar();
    this.avatarActual = null;
    this.avatarCargado = null;
    this.avatarUrl.set(null);
  }

  private aplicar(perfil: PerfilResponse): void {
    this.perfil.set(perfil);
    this.auth.updateNombreCompleto(perfil.nombreCompleto);
  }
}
