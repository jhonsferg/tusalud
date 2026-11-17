import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CuponRequest,
  CuponResponse,
  EstadisticasResponse,
  PagoResponse,
  RangoFechas,
  UsuarioAdminResponse,
} from '@tusalud/app/core/models';
import { AdminApiService } from '@tusalud/app/core/services/api/admin-api.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class AdminStore {
  private readonly api = inject(AdminApiService);
  private readonly rolActual = signal<string | null>(null);
  private readonly busquedaActual = signal<string | null>(null);

  readonly estadisticas = new AsyncState<EstadisticasResponse>();
  readonly cupones = new AsyncState<CuponResponse[]>();
  readonly pagos = new AsyncState<PagoResponse[]>();
  readonly usuarios = new AsyncState<UsuarioAdminResponse[]>();

  readonly filtroRol = this.rolActual.asReadonly();
  readonly filtroBusqueda = this.busquedaActual.asReadonly();
  readonly totalRecaudado = computed(() =>
    (this.pagos.value() ?? [])
      .filter((pago) => pago.estado === 'PAGADO')
      .reduce((total, pago) => total + pago.total, 0),
  );

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarEstadisticas(): void {
    this.estadisticas.load(this.api.estadisticas());
  }

  cargarCupones(): void {
    this.cupones.load(this.api.listarCupones());
  }

  guardarCupon(request: CuponRequest, id?: number): Observable<CuponResponse> {
    const operacion =
      id === undefined ? this.api.crearCupon(request) : this.api.actualizarCupon(id, request);
    return operacion.pipe(tap(() => this.cargarCupones()));
  }

  eliminarCupon(id: number): Observable<void> {
    return this.api
      .eliminarCupon(id)
      .pipe(tap(() => this.cupones.update((items) => items?.filter((item) => item.id !== id))));
  }

  cargarPagos(rango: RangoFechas = {}): void {
    this.pagos.load(this.api.listarPagos(rango));
  }

  cargarUsuarios(rol: string | null = null, q: string | null = null): void {
    this.rolActual.set(rol);
    this.busquedaActual.set(q);
    this.usuarios.load(this.api.listarUsuarios(rol, q));
  }

  cambiarEstadoUsuario(
    id: number,
    estado: 'ACTIVO' | 'INACTIVO',
  ): Observable<UsuarioAdminResponse> {
    return this.api
      .cambiarEstadoUsuario(id, estado)
      .pipe(
        tap((usuario) =>
          this.usuarios.update((items) => items?.map((item) => (item.id === id ? usuario : item))),
        ),
      );
  }

  reset(): void {
    this.rolActual.set(null);
    this.busquedaActual.set(null);
    this.estadisticas.reset();
    this.cupones.reset();
    this.pagos.reset();
    this.usuarios.reset();
  }
}
