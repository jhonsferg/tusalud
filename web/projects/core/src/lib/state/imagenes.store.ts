import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AgendaImagenResponse,
  ArchivoImagenResponse,
  EstudioImagenRequest,
  EstudioImagenResponse,
  FiltroOrdenesImagen,
  InformeImagenRequest,
  InformeImagenResponse,
  MiFirmaImagenResponse,
  OrdenImagenRequest,
  OrdenImagenResponse,
  ProgramarImagenRequest,
  RealizarImagenRequest,
  ResumenImagenesResponse,
} from '../models';
import { ImagenesApiService } from '../services/api/imagenes-api.service';
import { AsyncState } from './async-state';
import { resetOnLogout } from './session-scoped';

@Injectable({ providedIn: 'root' })
export class ImagenesStore {
  private readonly api = inject(ImagenesApiService);

  readonly resumen = new AsyncState<ResumenImagenesResponse>();
  readonly estudios = new AsyncState<EstudioImagenResponse[]>();
  readonly ordenes = new AsyncState<OrdenImagenResponse[]>();
  readonly orden = new AsyncState<OrdenImagenResponse>();
  readonly informes = new AsyncState<InformeImagenResponse[]>();
  readonly misEstudios = new AsyncState<OrdenImagenResponse[]>();
  readonly ordenesDePaciente = new AsyncState<OrdenImagenResponse[]>();
  readonly agenda = new AsyncState<AgendaImagenResponse[]>();
  readonly firma = new AsyncState<MiFirmaImagenResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  cargarResumen(): void {
    this.resumen.load(this.api.resumen());
  }

  cargarEstudios(
    filtro: { q?: string | null; modalidad?: string | null; activo?: boolean | null } = {},
  ): void {
    this.estudios.load(this.api.listarEstudios(filtro));
  }

  crearEstudio(request: EstudioImagenRequest): Observable<EstudioImagenResponse> {
    return this.api
      .crearEstudio(request)
      .pipe(tap((estudio) => this.estudios.update((items) => [estudio, ...(items ?? [])])));
  }

  actualizarEstudio(id: string, request: EstudioImagenRequest): Observable<EstudioImagenResponse> {
    return this.api
      .actualizarEstudio(id, request)
      .pipe(
        tap((estudio) =>
          this.estudios.update((items) => items?.map((item) => (item.id === id ? estudio : item))),
        ),
      );
  }

  cargarOrdenes(filtro: FiltroOrdenesImagen = {}): void {
    this.ordenes.load(this.api.listarOrdenes(filtro));
  }

  cargarOrden(id: string): void {
    this.orden.load(this.api.obtenerOrden(id));
  }

  cargarInformes(id: string): void {
    this.informes.load(this.api.historialInformes(id));
  }

  cargarMisEstudios(): void {
    this.misEstudios.load(this.api.misEstudios());
  }

  cargarOrdenesDePaciente(pacienteId: string): void {
    this.ordenesDePaciente.load(this.api.ordenesDePaciente(pacienteId));
  }

  cargarAgenda(desde: string, hasta: string): void {
    this.agenda.load(this.api.agenda(desde, hasta));
  }

  cargarFirma(): void {
    this.firma.load(this.api.miFirma());
  }

  crearOrden(request: OrdenImagenRequest): Observable<OrdenImagenResponse> {
    return this.api.crearOrden(request);
  }

  programar(id: string, request: ProgramarImagenRequest): Observable<OrdenImagenResponse> {
    return this.api.programar(id, request).pipe(tap((orden) => this.orden.set(orden)));
  }

  realizar(id: string, request: RealizarImagenRequest): Observable<OrdenImagenResponse> {
    return this.api.realizar(id, request).pipe(tap((orden) => this.orden.set(orden)));
  }

  informar(id: string, request: InformeImagenRequest): Observable<OrdenImagenResponse> {
    return this.api.informar(id, request).pipe(
      tap((orden) => {
        this.orden.set(orden);
        this.cargarInformes(id);
      }),
    );
  }

  cancelar(id: string, motivo: string): Observable<OrdenImagenResponse> {
    return this.api.cancelar(id, motivo).pipe(tap((orden) => this.orden.set(orden)));
  }

  subirArchivos(id: string, archivos: readonly File[]): Observable<ArchivoImagenResponse[]> {
    return this.api
      .subirArchivos(id, archivos)
      .pipe(
        tap((lista) =>
          this.orden.update((orden) => (orden ? { ...orden, archivos: lista } : orden)),
        ),
      );
  }

  eliminarArchivo(ordenId: string, archivoId: string): Observable<void> {
    return this.api
      .eliminarArchivo(ordenId, archivoId)
      .pipe(
        tap(() =>
          this.orden.update((orden) =>
            orden
              ? { ...orden, archivos: orden.archivos.filter((archivo) => archivo.id !== archivoId) }
              : orden,
          ),
        ),
      );
  }

  descargarArchivo(ordenId: string, archivoId: string): Observable<Blob> {
    return this.api.descargarArchivo(ordenId, archivoId);
  }

  reset(): void {
    this.resumen.reset();
    this.estudios.reset();
    this.ordenes.reset();
    this.orden.reset();
    this.informes.reset();
    this.misEstudios.reset();
    this.ordenesDePaciente.reset();
    this.agenda.reset();
    this.firma.reset();
  }
}
