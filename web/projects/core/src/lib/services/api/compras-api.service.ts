import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  ComparativaResponse,
  ComprasAuditoria,
  ComprobanteProveedorRequest,
  ComprobanteProveedorResponse,
  ConfiguracionCompras,
  CotizacionRequest,
  CotizacionResponse,
  DevolucionRequest,
  DevolucionResponse,
  FiltroComprobantes,
  FiltroOrdenesCompra,
  FiltroRequisiciones,
  OrdenCompraDetalle,
  OrdenCompraRequest,
  OrdenCompraResumen,
  PagoProveedorRequest,
  RecepcionDetalle,
  RecepcionRequest,
  RecepcionResumen,
  ReporteCompraProveedor,
  RequisicionDetalle,
  RequisicionRequest,
  RequisicionResumen,
  ResumenCompras,
  ResumenCuentasPorPagar,
  SugerenciaRequisicion,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class ComprasApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/compras`;

  configuracion(): Observable<ConfiguracionCompras> {
    return this.http.get<ConfiguracionCompras>(`${this.baseUrl}/configuracion`);
  }

  resumen(): Observable<ResumenCompras> {
    return this.http.get<ResumenCompras>(`${this.baseUrl}/resumen`);
  }

  requisiciones(filtro: FiltroRequisiciones = {}): Observable<RequisicionResumen[]> {
    return this.http.get<RequisicionResumen[]>(`${this.baseUrl}/requisiciones`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  sugeridas(): Observable<SugerenciaRequisicion[]> {
    return this.http.get<SugerenciaRequisicion[]>(`${this.baseUrl}/requisiciones/sugeridas`);
  }

  requisicion(id: string): Observable<RequisicionDetalle> {
    return this.http.get<RequisicionDetalle>(`${this.baseUrl}/requisiciones/${id}`);
  }

  crearRequisicion(request: RequisicionRequest): Observable<RequisicionDetalle> {
    return this.http.post<RequisicionDetalle>(`${this.baseUrl}/requisiciones`, request);
  }

  actualizarRequisicion(id: string, request: RequisicionRequest): Observable<RequisicionDetalle> {
    return this.http.put<RequisicionDetalle>(`${this.baseUrl}/requisiciones/${id}`, request);
  }

  enviarRequisicion(id: string): Observable<RequisicionDetalle> {
    return this.http.post<RequisicionDetalle>(`${this.baseUrl}/requisiciones/${id}/enviar`, {});
  }

  rechazarRequisicion(id: string, motivo: string): Observable<RequisicionDetalle> {
    return this.http.post<RequisicionDetalle>(`${this.baseUrl}/requisiciones/${id}/rechazar`, {
      motivo,
    });
  }

  anularRequisicion(id: string, motivo: string): Observable<RequisicionDetalle> {
    return this.http.post<RequisicionDetalle>(`${this.baseUrl}/requisiciones/${id}/anular`, {
      motivo,
    });
  }

  cotizaciones(requisicionId: string): Observable<CotizacionResponse[]> {
    return this.http.get<CotizacionResponse[]>(
      `${this.baseUrl}/requisiciones/${requisicionId}/cotizaciones`,
    );
  }

  comparativa(requisicionId: string): Observable<ComparativaResponse> {
    return this.http.get<ComparativaResponse>(
      `${this.baseUrl}/requisiciones/${requisicionId}/comparativa`,
    );
  }

  registrarCotizacion(
    requisicionId: string,
    request: CotizacionRequest,
  ): Observable<CotizacionResponse> {
    return this.http.post<CotizacionResponse>(
      `${this.baseUrl}/requisiciones/${requisicionId}/cotizaciones`,
      request,
    );
  }

  seleccionarCotizacion(id: string): Observable<CotizacionResponse> {
    return this.http.post<CotizacionResponse>(`${this.baseUrl}/cotizaciones/${id}/seleccionar`, {});
  }

  ordenes(filtro: FiltroOrdenesCompra = {}): Observable<OrdenCompraResumen[]> {
    return this.http.get<OrdenCompraResumen[]>(`${this.baseUrl}/ordenes`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  orden(id: string): Observable<OrdenCompraDetalle> {
    return this.http.get<OrdenCompraDetalle>(`${this.baseUrl}/ordenes/${id}`);
  }

  crearOrden(request: OrdenCompraRequest): Observable<OrdenCompraDetalle> {
    return this.http.post<OrdenCompraDetalle>(`${this.baseUrl}/ordenes`, request);
  }

  ordenDesdeCotizacion(cotizacionId: string): Observable<OrdenCompraDetalle> {
    return this.http.post<OrdenCompraDetalle>(
      `${this.baseUrl}/ordenes/desde-cotizacion/${cotizacionId}`,
      {},
    );
  }

  actualizarOrden(id: string, request: OrdenCompraRequest): Observable<OrdenCompraDetalle> {
    return this.http.put<OrdenCompraDetalle>(`${this.baseUrl}/ordenes/${id}`, request);
  }

  enviarAprobacion(id: string): Observable<OrdenCompraDetalle> {
    return this.http.post<OrdenCompraDetalle>(
      `${this.baseUrl}/ordenes/${id}/enviar-aprobacion`,
      {},
    );
  }

  aprobarOrden(id: string): Observable<OrdenCompraDetalle> {
    return this.http.post<OrdenCompraDetalle>(`${this.baseUrl}/ordenes/${id}/aprobar`, {});
  }

  rechazarOrden(id: string, motivo: string): Observable<OrdenCompraDetalle> {
    return this.http.post<OrdenCompraDetalle>(`${this.baseUrl}/ordenes/${id}/rechazar`, {
      motivo,
    });
  }

  anularOrden(id: string, motivo: string): Observable<OrdenCompraDetalle> {
    return this.http.post<OrdenCompraDetalle>(`${this.baseUrl}/ordenes/${id}/anular`, { motivo });
  }

  cerrarOrden(id: string, motivo: string): Observable<OrdenCompraDetalle> {
    return this.http.post<OrdenCompraDetalle>(`${this.baseUrl}/ordenes/${id}/cerrar`, { motivo });
  }

  auditoriaOrden(id: string): Observable<ComprasAuditoria[]> {
    return this.http.get<ComprasAuditoria[]>(`${this.baseUrl}/ordenes/${id}/auditoria`);
  }

  recibir(ordenId: string, request: RecepcionRequest): Observable<RecepcionDetalle> {
    return this.http.post<RecepcionDetalle>(
      `${this.baseUrl}/ordenes/${ordenId}/recepciones`,
      request,
    );
  }

  recepciones(
    filtro: {
      ordenId?: string | null;
      soloDiscrepancias?: boolean;
      desde?: string | null;
      hasta?: string | null;
    } = {},
  ): Observable<RecepcionResumen[]> {
    return this.http.get<RecepcionResumen[]>(`${this.baseUrl}/recepciones`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  recepcion(id: string): Observable<RecepcionDetalle> {
    return this.http.get<RecepcionDetalle>(`${this.baseUrl}/recepciones/${id}`);
  }

  devoluciones(proveedorId?: string | null): Observable<DevolucionResponse[]> {
    return this.http.get<DevolucionResponse[]>(`${this.baseUrl}/devoluciones`, {
      params: toHttpParams({ proveedorId }),
    });
  }

  devolver(request: DevolucionRequest): Observable<DevolucionResponse> {
    return this.http.post<DevolucionResponse>(`${this.baseUrl}/devoluciones`, request);
  }

  aplicarCredito(devolucionId: string, comprobanteId: string): Observable<DevolucionResponse> {
    return this.http.post<DevolucionResponse>(
      `${this.baseUrl}/devoluciones/${devolucionId}/aplicar-credito`,
      { comprobanteId },
    );
  }

  comprobantes(filtro: FiltroComprobantes = {}): Observable<ComprobanteProveedorResponse[]> {
    return this.http.get<ComprobanteProveedorResponse[]>(`${this.baseUrl}/comprobantes`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  comprobante(id: string): Observable<ComprobanteProveedorResponse> {
    return this.http.get<ComprobanteProveedorResponse>(`${this.baseUrl}/comprobantes/${id}`);
  }

  registrarComprobante(
    request: ComprobanteProveedorRequest,
  ): Observable<ComprobanteProveedorResponse> {
    return this.http.post<ComprobanteProveedorResponse>(`${this.baseUrl}/comprobantes`, request);
  }

  pagar(id: string, request: PagoProveedorRequest): Observable<ComprobanteProveedorResponse> {
    return this.http.post<ComprobanteProveedorResponse>(
      `${this.baseUrl}/comprobantes/${id}/pagos`,
      request,
    );
  }

  anularComprobante(id: string, motivo: string): Observable<ComprobanteProveedorResponse> {
    return this.http.post<ComprobanteProveedorResponse>(
      `${this.baseUrl}/comprobantes/${id}/anular`,
      { motivo },
    );
  }

  resumenCuentasPorPagar(): Observable<ResumenCuentasPorPagar> {
    return this.http.get<ResumenCuentasPorPagar>(`${this.baseUrl}/cuentas-por-pagar/resumen`);
  }

  reportePorProveedor(
    filtro: { desde?: string | null; hasta?: string | null; proveedorId?: string | null } = {},
  ): Observable<ReporteCompraProveedor[]> {
    return this.http.get<ReporteCompraProveedor[]>(`${this.baseUrl}/reportes/por-proveedor`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  auditoria(entidad?: string | null): Observable<ComprasAuditoria[]> {
    return this.http.get<ComprasAuditoria[]>(`${this.baseUrl}/auditoria`, {
      params: toHttpParams({ entidad }),
    });
  }
}
