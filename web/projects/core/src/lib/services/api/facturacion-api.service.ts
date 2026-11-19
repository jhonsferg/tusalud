import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  ArchivoDescargado,
  ConfiguracionFiscal,
  ConfiguracionFiscalRequest,
  CorreccionClienteRequest,
  DocumentoElectronicoDetalle,
  DocumentoElectronicoResumen,
  EstadoFacturacion,
  FiltroDocumentos,
  MotivoNota,
  NotaRequest,
  RegistroVentas,
  ResumenSunat,
  SerieComprobante,
  SerieRequest,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class FacturacionApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/facturacion`;

  estado(): Observable<EstadoFacturacion> {
    return this.http.get<EstadoFacturacion>(`${this.baseUrl}/estado`);
  }

  configuracion(): Observable<ConfiguracionFiscal> {
    return this.http.get<ConfiguracionFiscal>(`${this.baseUrl}/configuracion`);
  }

  guardarConfiguracion(request: ConfiguracionFiscalRequest): Observable<ConfiguracionFiscal> {
    return this.http.put<ConfiguracionFiscal>(`${this.baseUrl}/configuracion`, request);
  }

  series(): Observable<SerieComprobante[]> {
    return this.http.get<SerieComprobante[]>(`${this.baseUrl}/series`);
  }

  crearSerie(request: SerieRequest): Observable<SerieComprobante> {
    return this.http.post<SerieComprobante>(`${this.baseUrl}/series`, request);
  }

  estadoSerie(id: string, activa: boolean): Observable<SerieComprobante> {
    return this.http.patch<SerieComprobante>(`${this.baseUrl}/series/${id}`, { activa });
  }

  motivos(): Observable<MotivoNota[]> {
    return this.http.get<MotivoNota[]>(`${this.baseUrl}/motivos`);
  }

  documentos(filtro: FiltroDocumentos = {}): Observable<DocumentoElectronicoResumen[]> {
    return this.http.get<DocumentoElectronicoResumen[]>(`${this.baseUrl}/documentos`, {
      params: toHttpParams({
        desde: filtro.desde,
        hasta: filtro.hasta,
        tipo: filtro.tipo,
        estado: filtro.estado,
        q: filtro.q,
      }),
    });
  }

  detalle(id: string): Observable<DocumentoElectronicoDetalle> {
    return this.http.get<DocumentoElectronicoDetalle>(`${this.baseUrl}/documentos/${id}`);
  }

  porComprobante(comprobanteId: string): Observable<DocumentoElectronicoDetalle> {
    return this.http.get<DocumentoElectronicoDetalle>(
      `${this.baseUrl}/documentos/por-comprobante/${comprobanteId}`,
    );
  }

  pdf(id: string): Observable<ArchivoDescargado> {
    return this.descargar(`${this.baseUrl}/documentos/${id}/pdf`, 'comprobante.pdf');
  }

  xml(id: string): Observable<ArchivoDescargado> {
    return this.descargar(`${this.baseUrl}/documentos/${id}/xml`, 'comprobante.xml');
  }

  cdr(id: string): Observable<ArchivoDescargado> {
    return this.descargar(`${this.baseUrl}/documentos/${id}/cdr`, 'constancia.zip');
  }

  reenviar(id: string): Observable<DocumentoElectronicoDetalle> {
    return this.http.post<DocumentoElectronicoDetalle>(
      `${this.baseUrl}/documentos/${id}/reenviar`,
      null,
    );
  }

  corregirCliente(
    id: string,
    request: CorreccionClienteRequest,
  ): Observable<DocumentoElectronicoDetalle> {
    return this.http.put<DocumentoElectronicoDetalle>(
      `${this.baseUrl}/documentos/${id}/cliente`,
      request,
    );
  }

  baja(id: string, motivo: string): Observable<DocumentoElectronicoDetalle> {
    return this.http.post<DocumentoElectronicoDetalle>(`${this.baseUrl}/documentos/${id}/baja`, {
      motivo,
    });
  }

  nota(request: NotaRequest): Observable<DocumentoElectronicoDetalle> {
    return this.http.post<DocumentoElectronicoDetalle>(`${this.baseUrl}/notas`, request);
  }

  resumenes(): Observable<ResumenSunat[]> {
    return this.http.get<ResumenSunat[]>(`${this.baseUrl}/resumenes`);
  }

  generarResumen(fecha: string): Observable<ResumenSunat> {
    return this.http.post<ResumenSunat>(`${this.baseUrl}/resumenes`, { fecha });
  }

  procesarCola(): Observable<{ procesados: number }> {
    return this.http.post<{ procesados: number }>(`${this.baseUrl}/cola/procesar`, null);
  }

  registroVentas(desde?: string, hasta?: string): Observable<RegistroVentas> {
    return this.http.get<RegistroVentas>(`${this.baseUrl}/registro-ventas`, {
      params: toHttpParams({ desde, hasta }),
    });
  }

  registroVentasExcel(desde?: string, hasta?: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.baseUrl}/registro-ventas/excel`, {
        params: toHttpParams({ desde, hasta }),
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'registro-ventas.xlsx')));
  }

  misComprobantes(): Observable<DocumentoElectronicoResumen[]> {
    return this.http.get<DocumentoElectronicoResumen[]>(`${this.baseUrl}/mis-comprobantes`);
  }

  private descargar(url: string, nombre: string): Observable<ArchivoDescargado> {
    return this.http
      .get(url, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, nombre)));
  }
}
