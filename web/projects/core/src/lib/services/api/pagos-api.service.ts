import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  CuponValidacionResponse,
  MetodoPagoRequest,
  MetodoPagoResponse,
  PagoQrRequest,
  PagoQrResponse,
  PagoRequest,
  PagoResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class PagosApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  listarMetodos(): Observable<MetodoPagoResponse[]> {
    return this.http.get<MetodoPagoResponse[]>(`${this.apiUrl}/metodos-pago`);
  }

  crearMetodo(request: MetodoPagoRequest): Observable<MetodoPagoResponse> {
    return this.http.post<MetodoPagoResponse>(`${this.apiUrl}/metodos-pago`, request);
  }

  marcarPredeterminado(id: string): Observable<MetodoPagoResponse> {
    return this.http.put<MetodoPagoResponse>(
      `${this.apiUrl}/metodos-pago/${id}/predeterminado`,
      null,
    );
  }

  eliminarMetodo(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/metodos-pago/${id}`);
  }

  validarCupon(codigo: string, monto: number): Observable<CuponValidacionResponse> {
    return this.http.get<CuponValidacionResponse>(`${this.apiUrl}/cupones/validar`, {
      params: toHttpParams({ codigo, monto }),
    });
  }

  pagarCita(citaId: string, request: PagoRequest): Observable<PagoResponse> {
    return this.http.post<PagoResponse>(`${this.apiUrl}/citas/${citaId}/pago`, request);
  }

  listarPagos(): Observable<PagoResponse[]> {
    return this.http.get<PagoResponse[]>(`${this.apiUrl}/pagos`);
  }

  obtenerPago(id: string): Observable<PagoResponse> {
    return this.http.get<PagoResponse>(`${this.apiUrl}/pagos/${id}`);
  }

  crearPagoQr(request: PagoQrRequest): Observable<PagoQrResponse> {
    return this.http.post<PagoQrResponse>(`${this.apiUrl}/pagos/qr`, request);
  }

  consultarPagoQr(codigo: string): Observable<PagoQrResponse> {
    return this.http.get<PagoQrResponse>(`${this.apiUrl}/pagos/qr/${codigo}`);
  }

  simularConfirmacionQr(codigo: string): Observable<PagoQrResponse> {
    return this.http.post<PagoQrResponse>(
      `${this.apiUrl}/pagos/qr/${codigo}/simular-confirmacion`,
      null,
    );
  }

  cancelarPagoQr(codigo: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/pagos/qr/${codigo}`);
  }
}
