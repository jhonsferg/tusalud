import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const BANDEJA = roleGuard('ROLE_CAJA', 'ROLE_FINANZAS', 'ROLE_ADMIN', 'ROLE_GERENCIA');
const GESTION = roleGuard('ROLE_FINANZAS', 'ROLE_ADMIN');
const VENTAS = roleGuard('ROLE_FINANZAS', 'ROLE_ADMIN', 'ROLE_GERENCIA');

export const COMPROBANTES_ELECTRONICOS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Comprobantes electrónicos',
    canMatch: [BANDEJA],
    loadComponent: () => import('./bandeja-cpe/bandeja-cpe').then((m) => m.BandejaCpe),
  },
  {
    path: ':id',
    title: 'Comprobante electrónico',
    canMatch: [BANDEJA],
    loadComponent: () => import('./cpe-detalle/cpe-detalle').then((m) => m.CpeDetalle),
  },
];

export const RESUMENES_SUNAT_ROUTES: Routes = [
  {
    path: '',
    title: 'Resúmenes y bajas',
    canMatch: [GESTION],
    loadComponent: () => import('./resumenes-sunat/resumenes-sunat').then((m) => m.ResumenesSunat),
  },
];

export const CONFIGURACION_FISCAL_ROUTES: Routes = [
  {
    path: '',
    title: 'Configuración fiscal',
    canMatch: [GESTION],
    loadComponent: () =>
      import('./configuracion-fiscal/configuracion-fiscal').then(
        (m) => m.ConfiguracionFiscalPagina,
      ),
  },
];

export const REGISTRO_VENTAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Registro de ventas',
    canMatch: [VENTAS],
    loadComponent: () =>
      import('./registro-ventas/registro-ventas').then((m) => m.RegistroVentasPagina),
  },
];

export const MIS_COMPROBANTES_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis comprobantes',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('./mis-comprobantes/mis-comprobantes').then((m) => m.MisComprobantes),
  },
];
