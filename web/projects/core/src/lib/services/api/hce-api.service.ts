import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  AdendaRequest,
  ArchivoDescargado,
  CierreAtencion,
  CorreccionRequest,
  FusionPacientes,
  FusionPacientesRequest,
  HistorialAtencion,
  PacienteDuplicado,
  PoliticaConservacion,
  PoliticaConservacionRequest,
  VersionAtencion,
} from '../../models';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class HceApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/hce`;

  estado(atencionId: string): Observable<CierreAtencion> {
    return this.http.get<CierreAtencion>(`${this.base}/atenciones/${atencionId}/cierre`);
  }

  cerrar(atencionId: string): Observable<CierreAtencion> {
    return this.http.post<CierreAtencion>(`${this.base}/atenciones/${atencionId}/cierre`, {});
  }

  historial(atencionId: string): Observable<HistorialAtencion> {
    return this.http.get<HistorialAtencion>(`${this.base}/atenciones/${atencionId}/versiones`);
  }

  adenda(atencionId: string, request: AdendaRequest): Observable<VersionAtencion> {
    return this.http.post<VersionAtencion>(
      `${this.base}/atenciones/${atencionId}/adendas`,
      request,
    );
  }

  corregir(atencionId: string, request: CorreccionRequest): Observable<VersionAtencion> {
    return this.http.post<VersionAtencion>(
      `${this.base}/atenciones/${atencionId}/correcciones`,
      request,
    );
  }

  impresion(pacienteId: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.base}/pacientes/${pacienteId}/impresion`, {
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'historia-clinica.pdf')));
  }

  miHistoria(): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.base}/mi-historia/impresion`, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'historia-clinica.pdf')));
  }

  politicas(): Observable<PoliticaConservacion[]> {
    return this.http.get<PoliticaConservacion[]>(`${this.base}/conservacion`);
  }

  actualizarPolitica(
    tipo: string,
    request: PoliticaConservacionRequest,
  ): Observable<PoliticaConservacion> {
    return this.http.put<PoliticaConservacion>(`${this.base}/conservacion/${tipo}`, request);
  }

  duplicados(): Observable<PacienteDuplicado[]> {
    return this.http.get<PacienteDuplicado[]>(`${this.base}/pacientes/duplicados`);
  }

  fusiones(): Observable<FusionPacientes[]> {
    return this.http.get<FusionPacientes[]>(`${this.base}/pacientes/fusiones`);
  }

  fusionar(request: FusionPacientesRequest): Observable<FusionPacientes> {
    return this.http.post<FusionPacientes>(`${this.base}/pacientes/fusion`, request);
  }
}
