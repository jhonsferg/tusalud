import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  AccesoDatos,
  ArchivoDescargado,
  BancoDatos,
  BancoDatosRequest,
  ConsentimientoTratamientoEstado,
  EjecucionRetencion,
  EncargadoRequest,
  EncargadoTratamiento,
  EstadoAceptacion,
  EstadoArco,
  EstadoPrivacidad,
  FinalidadRequest,
  FinalidadTratamiento,
  HistorialConsentimiento,
  Incidente,
  IncidenteActualizacionRequest,
  IncidenteRequest,
  PoliticaPrivacidad,
  PoliticaPublica,
  PoliticaRequest,
  PoliticaRetencion,
  PreviaCancelacion,
  RegistroTratamientos,
  ResolucionArcoRequest,
  SolicitudArco,
  SolicitudArcoDetalle,
  SolicitudArcoRequest,
  TipoArco,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class ProteccionDatosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/privacidad`;
  private readonly gestion = `${this.baseUrl}/gestion`;

  politicaVigente(): Observable<PoliticaPublica> {
    return this.http.get<PoliticaPublica>(`${this.baseUrl}/politica`);
  }

  estadoAceptacion(): Observable<EstadoAceptacion> {
    return this.http.get<EstadoAceptacion>(`${this.baseUrl}/politica/estado`);
  }

  aceptarPolitica(): Observable<EstadoAceptacion> {
    return this.http.post<EstadoAceptacion>(`${this.baseUrl}/politica/aceptar`, null);
  }

  consentimientos(): Observable<ConsentimientoTratamientoEstado[]> {
    return this.http.get<ConsentimientoTratamientoEstado[]>(`${this.baseUrl}/consentimientos`);
  }

  historialConsentimientos(): Observable<HistorialConsentimiento[]> {
    return this.http.get<HistorialConsentimiento[]>(`${this.baseUrl}/consentimientos/historial`);
  }

  otorgar(finalidadId: string): Observable<ConsentimientoTratamientoEstado> {
    return this.http.post<ConsentimientoTratamientoEstado>(
      `${this.baseUrl}/consentimientos/${finalidadId}/otorgar`,
      {},
    );
  }

  revocar(finalidadId: string, motivo: string | null): Observable<ConsentimientoTratamientoEstado> {
    return this.http.post<ConsentimientoTratamientoEstado>(
      `${this.baseUrl}/consentimientos/${finalidadId}/revocar`,
      { motivo },
    );
  }

  accesos(): Observable<AccesoDatos[]> {
    return this.http.get<AccesoDatos[]>(`${this.baseUrl}/accesos`);
  }

  previaCancelacion(): Observable<PreviaCancelacion> {
    return this.http.get<PreviaCancelacion>(`${this.baseUrl}/cancelacion/previa`);
  }

  solicitar(request: SolicitudArcoRequest): Observable<SolicitudArco> {
    return this.http.post<SolicitudArco>(`${this.baseUrl}/solicitudes`, request);
  }

  mias(): Observable<SolicitudArco[]> {
    return this.http.get<SolicitudArco[]>(`${this.baseUrl}/solicitudes/mias`);
  }

  detalle(id: string): Observable<SolicitudArcoDetalle> {
    return this.http.get<SolicitudArcoDetalle>(`${this.baseUrl}/solicitudes/${id}`);
  }

  exportacion(id: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.baseUrl}/solicitudes/${id}/exportacion`, {
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'mis-datos.json')));
  }

  estado(): Observable<EstadoPrivacidad> {
    return this.http.get<EstadoPrivacidad>(`${this.gestion}/estado`);
  }

  bandeja(estado?: EstadoArco | null, tipo?: TipoArco | null): Observable<SolicitudArco[]> {
    return this.http.get<SolicitudArco[]>(`${this.gestion}/solicitudes`, {
      params: toHttpParams({ estado, tipo }),
    });
  }

  tomar(id: string): Observable<SolicitudArco> {
    return this.http.post<SolicitudArco>(`${this.gestion}/solicitudes/${id}/tomar`, null);
  }

  resolver(id: string, request: ResolucionArcoRequest): Observable<SolicitudArco> {
    return this.http.post<SolicitudArco>(`${this.gestion}/solicitudes/${id}/resolver`, request);
  }

  registro(): Observable<RegistroTratamientos> {
    return this.http.get<RegistroTratamientos>(`${this.gestion}/registro`);
  }

  guardarBanco(id: string | null, request: BancoDatosRequest): Observable<BancoDatos> {
    return id
      ? this.http.put<BancoDatos>(`${this.gestion}/bancos/${id}`, request)
      : this.http.post<BancoDatos>(`${this.gestion}/bancos`, request);
  }

  guardarFinalidad(id: string | null, request: FinalidadRequest): Observable<FinalidadTratamiento> {
    return id
      ? this.http.put<FinalidadTratamiento>(`${this.gestion}/finalidades/${id}`, request)
      : this.http.post<FinalidadTratamiento>(`${this.gestion}/finalidades`, request);
  }

  guardarEncargado(id: string | null, request: EncargadoRequest): Observable<EncargadoTratamiento> {
    return id
      ? this.http.put<EncargadoTratamiento>(`${this.gestion}/encargados/${id}`, request)
      : this.http.post<EncargadoTratamiento>(`${this.gestion}/encargados`, request);
  }

  politicas(): Observable<PoliticaPrivacidad[]> {
    return this.http.get<PoliticaPrivacidad[]>(`${this.gestion}/politicas`);
  }

  crearPolitica(request: PoliticaRequest): Observable<PoliticaPrivacidad> {
    return this.http.post<PoliticaPrivacidad>(`${this.gestion}/politicas`, request);
  }

  actualizarPolitica(id: string, request: PoliticaRequest): Observable<PoliticaPrivacidad> {
    return this.http.put<PoliticaPrivacidad>(`${this.gestion}/politicas/${id}`, request);
  }

  publicarPolitica(id: string): Observable<PoliticaPrivacidad> {
    return this.http.post<PoliticaPrivacidad>(`${this.gestion}/politicas/${id}/publicar`, null);
  }

  retenciones(): Observable<PoliticaRetencion[]> {
    return this.http.get<PoliticaRetencion[]>(`${this.gestion}/retencion`);
  }

  actualizarRetencion(
    id: string,
    plazoDias: number,
    activa: boolean,
  ): Observable<PoliticaRetencion> {
    return this.http.put<PoliticaRetencion>(`${this.gestion}/retencion/${id}`, {
      plazoDias,
      activa,
    });
  }

  ejecutarRetencion(): Observable<EjecucionRetencion[]> {
    return this.http.post<EjecucionRetencion[]>(`${this.gestion}/retencion/ejecutar`, null);
  }

  historialRetencion(): Observable<EjecucionRetencion[]> {
    return this.http.get<EjecucionRetencion[]>(`${this.gestion}/retencion/historial`);
  }

  incidentes(): Observable<Incidente[]> {
    return this.http.get<Incidente[]>(`${this.gestion}/incidentes`);
  }

  registrarIncidente(request: IncidenteRequest): Observable<Incidente> {
    return this.http.post<Incidente>(`${this.gestion}/incidentes`, request);
  }

  actualizarIncidente(id: string, request: IncidenteActualizacionRequest): Observable<Incidente> {
    return this.http.put<Incidente>(`${this.gestion}/incidentes/${id}`, request);
  }
}
