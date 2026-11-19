import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  ActualizarDocumentoRequest,
  ArchivoDescargado,
  ConsentimientoDetalle,
  ConsentimientoPlantilla,
  ConsentimientoPlantillaRequest,
  ConsentimientoResumen,
  ConsentimientoSolicitudRequest,
  DocumentoMedicoDetalle,
  DocumentoMedicoRequest,
  DocumentoMedicoResumen,
  EstablecimientoRequest,
  EstablecimientoSalud,
  FirmaConsentimientoRequest,
  ReferenciaDocumento,
  ReferenciaEstadoRequest,
  ResumenIngresoDocumentos,
  VerificacionPublica,
} from '../../models';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class DocumentosMedicosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_URL);

  crear(request: DocumentoMedicoRequest): Observable<DocumentoMedicoDetalle> {
    return this.http.post<DocumentoMedicoDetalle>(`${this.baseUrl}/documentos-medicos`, request);
  }

  epicrisisBorrador(ingresoId: string): Observable<DocumentoMedicoDetalle> {
    return this.http.post<DocumentoMedicoDetalle>(
      `${this.baseUrl}/documentos-medicos/epicrisis/borrador`,
      { ingresoId },
    );
  }

  actualizar(id: string, request: ActualizarDocumentoRequest): Observable<DocumentoMedicoDetalle> {
    return this.http.put<DocumentoMedicoDetalle>(
      `${this.baseUrl}/documentos-medicos/${id}`,
      request,
    );
  }

  emitir(id: string): Observable<DocumentoMedicoDetalle> {
    return this.http.post<DocumentoMedicoDetalle>(
      `${this.baseUrl}/documentos-medicos/${id}/emitir`,
      {},
    );
  }

  anular(id: string, motivo: string): Observable<DocumentoMedicoDetalle> {
    return this.http.post<DocumentoMedicoDetalle>(
      `${this.baseUrl}/documentos-medicos/${id}/anular`,
      {
        motivo,
      },
    );
  }

  estadoReferencia(id: string, request: ReferenciaEstadoRequest): Observable<ReferenciaDocumento> {
    return this.http.put<ReferenciaDocumento>(
      `${this.baseUrl}/documentos-medicos/${id}/referencia/estado`,
      request,
    );
  }

  obtener(id: string): Observable<DocumentoMedicoDetalle> {
    return this.http.get<DocumentoMedicoDetalle>(`${this.baseUrl}/documentos-medicos/${id}`);
  }

  pdf(id: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.baseUrl}/documentos-medicos/${id}/pdf`, {
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'documento.pdf')));
  }

  mios(): Observable<DocumentoMedicoResumen[]> {
    return this.http.get<DocumentoMedicoResumen[]>(`${this.baseUrl}/documentos-medicos/mios`);
  }

  emitidos(): Observable<DocumentoMedicoResumen[]> {
    return this.http.get<DocumentoMedicoResumen[]>(`${this.baseUrl}/documentos-medicos/emitidos`);
  }

  delPaciente(pacienteId: string): Observable<DocumentoMedicoResumen[]> {
    return this.http.get<DocumentoMedicoResumen[]>(`${this.baseUrl}/documentos-medicos`, {
      params: { pacienteId },
    });
  }

  resumenIngreso(ingresoId: string): Observable<ResumenIngresoDocumentos> {
    return this.http.get<ResumenIngresoDocumentos>(
      `${this.baseUrl}/documentos-medicos/ingreso/${ingresoId}/resumen`,
    );
  }

  establecimientos(): Observable<EstablecimientoSalud[]> {
    return this.http.get<EstablecimientoSalud[]>(`${this.baseUrl}/establecimientos-salud`);
  }

  crearEstablecimiento(request: EstablecimientoRequest): Observable<EstablecimientoSalud> {
    return this.http.post<EstablecimientoSalud>(`${this.baseUrl}/establecimientos-salud`, request);
  }

  actualizarEstablecimiento(
    id: string,
    request: EstablecimientoRequest,
  ): Observable<EstablecimientoSalud> {
    return this.http.put<EstablecimientoSalud>(
      `${this.baseUrl}/establecimientos-salud/${id}`,
      request,
    );
  }

  plantillasConsentimiento(): Observable<ConsentimientoPlantilla[]> {
    return this.http.get<ConsentimientoPlantilla[]>(`${this.baseUrl}/consentimientos/plantillas`);
  }

  crearPlantillaConsentimiento(
    request: ConsentimientoPlantillaRequest,
  ): Observable<ConsentimientoPlantilla> {
    return this.http.post<ConsentimientoPlantilla>(
      `${this.baseUrl}/consentimientos/plantillas`,
      request,
    );
  }

  actualizarPlantillaConsentimiento(
    id: string,
    request: ConsentimientoPlantillaRequest,
  ): Observable<ConsentimientoPlantilla> {
    return this.http.put<ConsentimientoPlantilla>(
      `${this.baseUrl}/consentimientos/plantillas/${id}`,
      request,
    );
  }

  solicitarConsentimiento(
    request: ConsentimientoSolicitudRequest,
  ): Observable<ConsentimientoResumen> {
    return this.http.post<ConsentimientoResumen>(`${this.baseUrl}/consentimientos`, request);
  }

  firmarConsentimiento(
    id: string,
    request: FirmaConsentimientoRequest,
  ): Observable<ConsentimientoDetalle> {
    return this.http.post<ConsentimientoDetalle>(
      `${this.baseUrl}/consentimientos/${id}/firma`,
      request,
    );
  }

  revocarConsentimiento(id: string, motivo: string): Observable<ConsentimientoDetalle> {
    return this.http.post<ConsentimientoDetalle>(`${this.baseUrl}/consentimientos/${id}/revocar`, {
      motivo,
    });
  }

  consentimientosMios(): Observable<ConsentimientoResumen[]> {
    return this.http.get<ConsentimientoResumen[]>(`${this.baseUrl}/consentimientos/mios`);
  }

  consentimientosDelPaciente(pacienteId: string): Observable<ConsentimientoResumen[]> {
    return this.http.get<ConsentimientoResumen[]>(`${this.baseUrl}/consentimientos`, {
      params: { pacienteId },
    });
  }

  consentimiento(id: string): Observable<ConsentimientoDetalle> {
    return this.http.get<ConsentimientoDetalle>(`${this.baseUrl}/consentimientos/${id}`);
  }

  consentimientoPdf(id: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.baseUrl}/consentimientos/${id}/pdf`, {
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'consentimiento.pdf')));
  }

  verificar(codigo: string): Observable<VerificacionPublica> {
    return this.http.get<VerificacionPublica>(
      `${this.baseUrl}/verificacion/${encodeURIComponent(codigo.trim())}`,
    );
  }
}
