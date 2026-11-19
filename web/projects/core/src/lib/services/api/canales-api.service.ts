import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  CanalExterno,
  ContactoCanal,
  EntregaNotificacion,
  EstadoRespaldos,
  FiltroEntregas,
  MensajeSimulado,
  MisCanales,
  PaginaEntregas,
  PlantillaMensaje,
  PlantillaMensajeRequest,
  PlantillasMensaje,
  PreferenciasCanalesRequest,
  ProveedoresCanales,
  RespaldoEjecucion,
  ResumenEntregas,
  SolicitudContactoRequest,
  TipoEjecucionRespaldo,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class CanalesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_URL);

  misCanales(): Observable<MisCanales> {
    return this.http.get<MisCanales>(`${this.baseUrl}/canales/mis-canales`);
  }

  solicitar(canal: CanalExterno, request: SolicitudContactoRequest): Observable<ContactoCanal> {
    return this.http.post<ContactoCanal>(
      `${this.baseUrl}/canales/contactos/${canal}/solicitar`,
      request,
    );
  }

  verificar(canal: CanalExterno, codigo: string): Observable<ContactoCanal> {
    return this.http.post<ContactoCanal>(`${this.baseUrl}/canales/contactos/${canal}/verificar`, {
      codigo,
    });
  }

  revocar(canal: CanalExterno): Observable<ContactoCanal> {
    return this.http.delete<ContactoCanal>(`${this.baseUrl}/canales/contactos/${canal}`);
  }

  guardarPreferencias(request: PreferenciasCanalesRequest): Observable<MisCanales> {
    return this.http.put<MisCanales>(`${this.baseUrl}/canales/preferencias`, request);
  }

  darDeBaja(token: string): Observable<{ canal: CanalExterno; etiqueta: string }> {
    return this.http.post<{ canal: CanalExterno; etiqueta: string }>(
      `${this.baseUrl}/canales/baja`,
      null,
      { params: { t: token } },
    );
  }

  entregas(filtro: FiltroEntregas = {}): Observable<PaginaEntregas> {
    return this.http.get<PaginaEntregas>(`${this.baseUrl}/canales/entregas`, {
      params: toHttpParams({
        canal: filtro.canal,
        estado: filtro.estado,
        desde: filtro.desde,
        hasta: filtro.hasta,
        pagina: filtro.pagina ?? 0,
        tamano: filtro.tamano ?? 20,
      }),
    });
  }

  resumenEntregas(dias = 7): Observable<ResumenEntregas> {
    return this.http.get<ResumenEntregas>(`${this.baseUrl}/canales/entregas/resumen`, {
      params: toHttpParams({ dias }),
    });
  }

  reintentar(id: string): Observable<EntregaNotificacion> {
    return this.http.post<EntregaNotificacion>(
      `${this.baseUrl}/canales/entregas/${id}/reintentar`,
      null,
    );
  }

  plantillas(): Observable<PlantillasMensaje> {
    return this.http.get<PlantillasMensaje>(`${this.baseUrl}/admin/canales/plantillas`);
  }

  actualizarPlantilla(
    tipo: string,
    canal: CanalExterno,
    request: PlantillaMensajeRequest,
  ): Observable<PlantillaMensaje> {
    return this.http.put<PlantillaMensaje>(
      `${this.baseUrl}/admin/canales/plantillas/${tipo}/${canal}`,
      request,
    );
  }

  restaurarPlantilla(tipo: string, canal: CanalExterno): Observable<PlantillaMensaje> {
    return this.http.delete<PlantillaMensaje>(
      `${this.baseUrl}/admin/canales/plantillas/${tipo}/${canal}`,
    );
  }

  historialPlantilla(tipo: string, canal: CanalExterno): Observable<PlantillaMensaje[]> {
    return this.http.get<PlantillaMensaje[]>(
      `${this.baseUrl}/admin/canales/plantillas/${tipo}/${canal}/historial`,
    );
  }

  proveedores(): Observable<ProveedoresCanales> {
    return this.http.get<ProveedoresCanales>(`${this.baseUrl}/admin/canales/proveedores`);
  }

  mensajesSimulados(limite = 20): Observable<MensajeSimulado[]> {
    return this.http.get<MensajeSimulado[]>(`${this.baseUrl}/admin/canales/simulacion`, {
      params: toHttpParams({ limite }),
    });
  }

  respaldos(tipo?: TipoEjecucionRespaldo | null, limite = 30): Observable<RespaldoEjecucion[]> {
    return this.http.get<RespaldoEjecucion[]>(`${this.baseUrl}/respaldos`, {
      params: toHttpParams({ tipo, limite }),
    });
  }

  estadoRespaldos(): Observable<EstadoRespaldos> {
    return this.http.get<EstadoRespaldos>(`${this.baseUrl}/respaldos/estado`);
  }
}
