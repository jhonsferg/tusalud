import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { EspecialidadRequest, EspecialidadResponse } from '../../models';

@Injectable({ providedIn: 'root' })
export class EspecialidadesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/especialidades`;

  listar(): Observable<EspecialidadResponse[]> {
    return this.http.get<EspecialidadResponse[]>(this.baseUrl);
  }

  obtener(id: number): Observable<EspecialidadResponse> {
    return this.http.get<EspecialidadResponse>(`${this.baseUrl}/${id}`);
  }

  crear(request: EspecialidadRequest): Observable<EspecialidadResponse> {
    return this.http.post<EspecialidadResponse>(this.baseUrl, request);
  }

  actualizar(id: number, request: EspecialidadRequest): Observable<EspecialidadResponse> {
    return this.http.put<EspecialidadResponse>(`${this.baseUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
