import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { SKIP_ERROR_NOTIFICATION } from '../../config/http-context';
import {
  Cie10FrecuenteResponse,
  Cie10ImportacionResponse,
  Cie10Response,
  Cie10ResumenResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class Cie10ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/cie10`;
  private readonly adminUrl = `${inject(API_URL)}/admin/cie10`;

  buscar(consulta: string, limite = 15): Observable<Cie10Response[]> {
    return this.http.get<Cie10Response[]>(this.baseUrl, {
      params: toHttpParams({ q: consulta, limite }),
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  obtener(codigo: string): Observable<Cie10Response> {
    return this.http.get<Cie10Response>(`${this.baseUrl}/${encodeURIComponent(codigo)}`);
  }

  frecuentes(limite = 8): Observable<Cie10FrecuenteResponse[]> {
    return this.http.get<Cie10FrecuenteResponse[]>(`${this.baseUrl}/mis-frecuentes`, {
      params: toHttpParams({ limite }),
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  resumen(): Observable<Cie10ResumenResponse> {
    return this.http.get<Cie10ResumenResponse>(`${this.adminUrl}/resumen`);
  }

  plantilla(): Observable<Blob> {
    return this.http.get(`${this.adminUrl}/plantilla.csv`, { responseType: 'blob' });
  }

  vistaPrevia(archivo: File, desactivarAusentes: boolean): Observable<Cie10ImportacionResponse> {
    const datos = new FormData();
    datos.append('archivo', archivo, archivo.name);
    datos.append('desactivarAusentes', String(desactivarAusentes));
    return this.http.post<Cie10ImportacionResponse>(
      `${this.adminUrl}/importaciones/vista-previa`,
      datos,
    );
  }

  importar(
    archivo: File,
    etiqueta: string,
    desactivarAusentes: boolean,
    omitirErrores: boolean,
  ): Observable<Cie10ImportacionResponse> {
    const datos = new FormData();
    datos.append('archivo', archivo, archivo.name);
    datos.append('etiqueta', etiqueta);
    datos.append('desactivarAusentes', String(desactivarAusentes));
    datos.append('omitirErrores', String(omitirErrores));
    return this.http.post<Cie10ImportacionResponse>(`${this.adminUrl}/importaciones`, datos);
  }
}
