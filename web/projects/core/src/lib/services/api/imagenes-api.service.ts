import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
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
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class ImagenesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/imagenes`;

  resumen(): Observable<ResumenImagenesResponse> {
    return this.http.get<ResumenImagenesResponse>(`${this.baseUrl}/resumen`);
  }

  listarEstudios(
    filtro: { q?: string | null; modalidad?: string | null; activo?: boolean | null } = {},
  ): Observable<EstudioImagenResponse[]> {
    return this.http.get<EstudioImagenResponse[]>(`${this.baseUrl}/estudios`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  crearEstudio(request: EstudioImagenRequest): Observable<EstudioImagenResponse> {
    return this.http.post<EstudioImagenResponse>(`${this.baseUrl}/estudios`, request);
  }

  actualizarEstudio(id: string, request: EstudioImagenRequest): Observable<EstudioImagenResponse> {
    return this.http.put<EstudioImagenResponse>(`${this.baseUrl}/estudios/${id}`, request);
  }

  listarOrdenes(filtro: FiltroOrdenesImagen = {}): Observable<OrdenImagenResponse[]> {
    return this.http.get<OrdenImagenResponse[]>(`${this.baseUrl}/ordenes`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  ordenesDePaciente(pacienteId: string): Observable<OrdenImagenResponse[]> {
    return this.http.get<OrdenImagenResponse[]>(`${this.baseUrl}/pacientes/${pacienteId}/ordenes`);
  }

  misEstudios(): Observable<OrdenImagenResponse[]> {
    return this.http.get<OrdenImagenResponse[]>(`${this.baseUrl}/mis-estudios`);
  }

  obtenerOrden(id: string): Observable<OrdenImagenResponse> {
    return this.http.get<OrdenImagenResponse>(`${this.baseUrl}/ordenes/${id}`);
  }

  crearOrden(request: OrdenImagenRequest): Observable<OrdenImagenResponse> {
    return this.http.post<OrdenImagenResponse>(`${this.baseUrl}/ordenes`, request);
  }

  programar(id: string, request: ProgramarImagenRequest): Observable<OrdenImagenResponse> {
    return this.http.put<OrdenImagenResponse>(`${this.baseUrl}/ordenes/${id}/programar`, request);
  }

  realizar(id: string, request: RealizarImagenRequest): Observable<OrdenImagenResponse> {
    return this.http.put<OrdenImagenResponse>(`${this.baseUrl}/ordenes/${id}/realizar`, request);
  }

  informar(id: string, request: InformeImagenRequest): Observable<OrdenImagenResponse> {
    return this.http.put<OrdenImagenResponse>(`${this.baseUrl}/ordenes/${id}/informe`, request);
  }

  historialInformes(id: string): Observable<InformeImagenResponse[]> {
    return this.http.get<InformeImagenResponse[]>(`${this.baseUrl}/ordenes/${id}/informes`);
  }

  cancelar(id: string, motivo: string | null): Observable<OrdenImagenResponse> {
    return this.http.put<OrdenImagenResponse>(`${this.baseUrl}/ordenes/${id}/cancelar`, {
      motivo,
    });
  }

  subirArchivos(id: string, archivos: readonly File[]): Observable<ArchivoImagenResponse[]> {
    const formulario = new FormData();
    for (const archivo of archivos) {
      formulario.append('archivos', archivo, archivo.name);
    }
    return this.http.post<ArchivoImagenResponse[]>(
      `${this.baseUrl}/ordenes/${id}/archivos`,
      formulario,
    );
  }

  descargarArchivo(ordenId: string, archivoId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/ordenes/${ordenId}/archivos/${archivoId}`, {
      responseType: 'blob',
    });
  }

  eliminarArchivo(ordenId: string, archivoId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/ordenes/${ordenId}/archivos/${archivoId}`);
  }

  agenda(desde: string, hasta: string): Observable<AgendaImagenResponse[]> {
    return this.http.get<AgendaImagenResponse[]>(`${this.baseUrl}/agenda`, {
      params: toHttpParams({ desde, hasta }),
    });
  }

  miFirma(): Observable<MiFirmaImagenResponse> {
    return this.http.get<MiFirmaImagenResponse>(`${this.baseUrl}/mi-firma`);
  }
}
