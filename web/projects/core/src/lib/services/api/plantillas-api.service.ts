import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { ArchivoDescargado, PlantillaResponse, PlantillaVersionResponse } from '../../models';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class PlantillasApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/plantillas`;

  listar(): Observable<PlantillaResponse[]> {
    return this.http.get<PlantillaResponse[]>(this.baseUrl);
  }

  descargar(codigo: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.baseUrl}/${codigo}/descargar`, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, `${codigo}.jrxml`)));
  }

  historial(codigo: string): Observable<PlantillaVersionResponse[]> {
    return this.http.get<PlantillaVersionResponse[]>(`${this.baseUrl}/${codigo}/historial`);
  }

  reemplazar(codigo: string, archivo: File, motivo?: string | null): Observable<PlantillaResponse> {
    const formulario = new FormData();
    formulario.append('archivo', archivo, archivo.name);
    if (motivo) {
      formulario.append('motivo', motivo);
    }
    return this.http.put<PlantillaResponse>(`${this.baseUrl}/${codigo}`, formulario);
  }

  restaurar(codigo: string, motivo?: string | null): Observable<PlantillaResponse> {
    let params = new HttpParams();
    if (motivo) {
      params = params.set('motivo', motivo);
    }
    return this.http.post<PlantillaResponse>(`${this.baseUrl}/${codigo}/restaurar`, null, {
      params,
    });
  }
}
