import { Injectable, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AjusteRequest,
  AlertasAlmacenResponse,
  FiltroMovimientos,
  IngresoRequest,
  IngresoResponse,
  LoteResponse,
  MovimientoResponse,
  ProductoRequest,
  ProductoResponse,
  ProveedorRequest,
  ProveedorResponse,
  ResumenAlmacenResponse,
  SalidaRequest,
} from '../models';
import { AlmacenApiService } from '../services/api/almacen-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class AlmacenStore {
  private readonly api = inject(AlmacenApiService);

  readonly resumen = new AsyncState<ResumenAlmacenResponse>();
  readonly alertas = new AsyncState<AlertasAlmacenResponse>();
  readonly productos = new AsyncState<ProductoResponse[]>();
  readonly lotes = new AsyncState<LoteResponse[]>();
  readonly proveedores = new AsyncState<ProveedorResponse[]>();
  readonly ingresos = new AsyncState<IngresoResponse[]>();
  readonly movimientos = new AsyncState<MovimientoResponse[]>();

  readonly proveedoresActivos = computed(() =>
    (this.proveedores.value() ?? []).filter((proveedor) => proveedor.activo),
  );
  readonly productosActivos = computed(() =>
    (this.productos.value() ?? []).filter((producto) => producto.activo),
  );

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarAlertas(): void {
    this.alertas.load(this.api.alertas());
  }

  cargarProductos(
    filtro: {
      q?: string | null;
      tipo?: string | null;
      bajoStock?: boolean | null;
      activo?: boolean | null;
    } = {},
  ): void {
    this.productos.load(this.api.listarProductos(filtro));
  }

  cargarLotes(productoId: string): void {
    this.lotes.load(this.api.listarLotes(productoId));
  }

  crearProducto(request: ProductoRequest): Observable<ProductoResponse> {
    return this.api
      .crearProducto(request)
      .pipe(tap((producto) => this.productos.update((items) => [producto, ...(items ?? [])])));
  }

  actualizarProducto(id: string, request: ProductoRequest): Observable<ProductoResponse> {
    return this.api
      .actualizarProducto(id, request)
      .pipe(
        tap((producto) =>
          this.productos.update((items) =>
            items?.map((item) => (item.id === id ? producto : item)),
          ),
        ),
      );
  }

  cargarProveedores(filtro: { q?: string | null; activo?: boolean | null } = {}): void {
    this.proveedores.load(this.api.listarProveedores(filtro));
  }

  crearProveedor(request: ProveedorRequest): Observable<ProveedorResponse> {
    return this.api
      .crearProveedor(request)
      .pipe(tap((proveedor) => this.proveedores.update((items) => [proveedor, ...(items ?? [])])));
  }

  actualizarProveedor(id: string, request: ProveedorRequest): Observable<ProveedorResponse> {
    return this.api
      .actualizarProveedor(id, request)
      .pipe(
        tap((proveedor) =>
          this.proveedores.update((items) =>
            items?.map((item) => (item.id === id ? proveedor : item)),
          ),
        ),
      );
  }

  cargarIngresos(): void {
    this.ingresos.load(this.api.listarIngresos());
  }

  registrarIngreso(request: IngresoRequest): Observable<IngresoResponse> {
    return this.api
      .registrarIngreso(request)
      .pipe(tap((ingreso) => this.ingresos.update((items) => [ingreso, ...(items ?? [])])));
  }

  registrarSalida(request: SalidaRequest): Observable<MovimientoResponse[]> {
    return this.api.registrarSalida(request);
  }

  registrarAjuste(request: AjusteRequest): Observable<MovimientoResponse> {
    return this.api.registrarAjuste(request);
  }

  cargarMovimientos(filtro: FiltroMovimientos = {}): void {
    this.movimientos.load(this.api.listarMovimientos(filtro));
  }

  reset(): void {
    this.resumen.reset();
    this.alertas.reset();
    this.productos.reset();
    this.lotes.reset();
    this.proveedores.reset();
    this.ingresos.reset();
    this.movimientos.reset();
  }
}
