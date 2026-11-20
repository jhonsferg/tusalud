import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  ArchivoDescargado,
  CarneVacunacion,
  Crecimiento,
  DosisRequest,
  EsquemaVacunacion,
  MedicionCrecimiento,
  MedicionRequest,
  PacienteClinicoResumen,
  RegistroVacuna,
  Vacuna,
} from '../../models';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class VacunacionApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/vacunacion`;

  vacunas(): Observable<Vacuna[]> {
    return this.http.get<Vacuna[]>(`${this.base}/vacunas`);
  }

  esquemaVigente(): Observable<EsquemaVacunacion> {
    return this.http.get<EsquemaVacunacion>(`${this.base}/esquema`);
  }

  esquemas(): Observable<EsquemaVacunacion[]> {
    return this.http.get<EsquemaVacunacion[]>(`${this.base}/esquemas`);
  }

  pacientes(q = ''): Observable<PacienteClinicoResumen[]> {
    const params = q.trim() ? new HttpParams().set('q', q.trim()) : new HttpParams();
    return this.http.get<PacienteClinicoResumen[]>(`${this.base}/pacientes`, { params });
  }

  carne(pacienteId: string): Observable<CarneVacunacion> {
    return this.http.get<CarneVacunacion>(`${this.base}/pacientes/${pacienteId}/carne`);
  }

  carneMio(): Observable<CarneVacunacion> {
    return this.http.get<CarneVacunacion>(`${this.base}/mio`);
  }

  pdf(pacienteId: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.base}/pacientes/${pacienteId}/carne/pdf`, {
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'carne-vacunacion.pdf')));
  }

  registrar(request: DosisRequest): Observable<RegistroVacuna> {
    return this.http.post<RegistroVacuna>(`${this.base}/dosis`, request);
  }

  anular(id: string, motivo: string): Observable<RegistroVacuna> {
    return this.http.post<RegistroVacuna>(`${this.base}/dosis/${id}/anular`, { motivo });
  }
}

@Injectable({ providedIn: 'root' })
export class CrecimientoApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/crecimiento`;

  paciente(pacienteId: string): Observable<Crecimiento> {
    return this.http.get<Crecimiento>(`${this.base}/pacientes/${pacienteId}`);
  }

  mio(): Observable<Crecimiento> {
    return this.http.get<Crecimiento>(`${this.base}/mio`);
  }

  registrar(request: MedicionRequest): Observable<MedicionCrecimiento> {
    return this.http.post<MedicionCrecimiento>(`${this.base}/mediciones`, request);
  }

  anular(id: string, motivo: string): Observable<MedicionCrecimiento> {
    return this.http.post<MedicionCrecimiento>(`${this.base}/mediciones/${id}/anular`, { motivo });
  }
}
