import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  AlertasReceta,
  AlertasRecetaRequest,
  ArchivoDescargado,
  DispensacionResponse,
  DispensarRecetaRequest,
  EmitirRecetaRequest,
  ImportacionCatalogo,
  InteraccionMedicamentosa,
  InteraccionRequest,
  Medicamento,
  MedicamentoRequest,
  PrincipioActivo,
  PrincipioActivoRequest,
  RecetaElectronica,
  RecetaFarmacia,
  RecetaResumen,
} from '../../models';
import { archivoDeRespuesta } from './documentos-api.service';

export interface VerificacionFirma {
  numero: string;
  hashCoincide: boolean;
  firmaValida: boolean;
  certificadoHuella: string;
  dePrueba: boolean;
}

@Injectable({ providedIn: 'root' })
export class RecetasApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/recetas`;

  alertas(request: AlertasRecetaRequest): Observable<AlertasReceta> {
    return this.http.post<AlertasReceta>(`${this.base}/alertas`, request);
  }

  emitir(request: EmitirRecetaRequest): Observable<RecetaElectronica> {
    return this.http.post<RecetaElectronica>(this.base, request);
  }

  obtener(id: string): Observable<RecetaElectronica> {
    return this.http.get<RecetaElectronica>(`${this.base}/${id}`);
  }

  mias(): Observable<RecetaResumen[]> {
    return this.http.get<RecetaResumen[]>(`${this.base}/mias`);
  }

  emitidas(): Observable<RecetaResumen[]> {
    return this.http.get<RecetaResumen[]>(`${this.base}/emitidas`);
  }

  delPaciente(pacienteId: string): Observable<RecetaResumen[]> {
    return this.http.get<RecetaResumen[]>(`${this.base}/paciente/${pacienteId}`);
  }

  deLaAtencion(atencionId: string): Observable<RecetaResumen[]> {
    return this.http.get<RecetaResumen[]>(`${this.base}/atencion/${atencionId}`);
  }

  firma(id: string): Observable<VerificacionFirma> {
    return this.http.get<VerificacionFirma>(`${this.base}/${id}/firma`);
  }

  anular(id: string, motivo: string): Observable<RecetaElectronica> {
    return this.http.post<RecetaElectronica>(`${this.base}/${id}/anular`, { motivo });
  }

  pdf(id: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.base}/${id}/pdf`, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'receta-electronica.pdf')));
  }

  paraDispensar(q = ''): Observable<RecetaFarmacia[]> {
    const params = q.trim() ? new HttpParams().set('q', q.trim()) : new HttpParams();
    return this.http.get<RecetaFarmacia[]>(`${this.base}/farmacia`, { params });
  }

  detalleFarmacia(id: string): Observable<RecetaFarmacia> {
    return this.http.get<RecetaFarmacia>(`${this.base}/farmacia/${id}`);
  }

  dispensar(id: string, request: DispensarRecetaRequest): Observable<DispensacionResponse> {
    return this.http.post<DispensacionResponse>(`${this.base}/farmacia/${id}/dispensar`, request);
  }
}

@Injectable({ providedIn: 'root' })
export class MedicamentosApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/medicamentos`;

  buscar(q = ''): Observable<Medicamento[]> {
    const params = q.trim() ? new HttpParams().set('q', q.trim()) : new HttpParams();
    return this.http.get<Medicamento[]>(this.base, { params });
  }

  obtener(id: string): Observable<Medicamento> {
    return this.http.get<Medicamento>(`${this.base}/${id}`);
  }

  crear(request: MedicamentoRequest): Observable<Medicamento> {
    return this.http.post<Medicamento>(this.base, request);
  }

  actualizar(id: string, request: MedicamentoRequest): Observable<Medicamento> {
    return this.http.put<Medicamento>(`${this.base}/${id}`, request);
  }

  principios(): Observable<PrincipioActivo[]> {
    return this.http.get<PrincipioActivo[]>(`${this.base}/principios-activos`);
  }

  crearPrincipio(request: PrincipioActivoRequest): Observable<PrincipioActivo> {
    return this.http.post<PrincipioActivo>(`${this.base}/principios-activos`, request);
  }

  actualizarPrincipio(id: string, request: PrincipioActivoRequest): Observable<PrincipioActivo> {
    return this.http.put<PrincipioActivo>(`${this.base}/principios-activos/${id}`, request);
  }

  interacciones(): Observable<InteraccionMedicamentosa[]> {
    return this.http.get<InteraccionMedicamentosa[]>(`${this.base}/interacciones`);
  }

  crearInteraccion(request: InteraccionRequest): Observable<InteraccionMedicamentosa> {
    return this.http.post<InteraccionMedicamentosa>(`${this.base}/interacciones`, request);
  }

  eliminarInteraccion(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/interacciones/${id}`);
  }

  plantillaCsv(): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.base}/importaciones/plantilla`, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'plantilla-medicamentos.csv')));
  }

  importar(archivo: File, soloValidar: boolean): Observable<ImportacionCatalogo> {
    const datos = new FormData();
    datos.append('archivo', archivo);
    const ruta = soloValidar ? '/importaciones/vista-previa' : '/importaciones';
    return this.http.post<ImportacionCatalogo>(`${this.base}${ruta}`, datos);
  }
}
