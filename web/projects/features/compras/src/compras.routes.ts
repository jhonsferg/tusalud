import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const REQUISICIONES = roleGuard(
  'ROLE_ALMACEN',
  'ROLE_FARMACIA',
  'ROLE_FINANZAS',
  'ROLE_GERENCIA',
  'ROLE_ADMIN',
);
const SOLICITANTES = roleGuard('ROLE_ALMACEN', 'ROLE_FARMACIA');
const ORDENES = roleGuard('ROLE_ALMACEN', 'ROLE_FINANZAS', 'ROLE_GERENCIA', 'ROLE_ADMIN');
const COMPRADORES = roleGuard('ROLE_ALMACEN');
const FINANCIERO = roleGuard('ROLE_FINANZAS', 'ROLE_GERENCIA', 'ROLE_ADMIN');

export const REQUISICIONES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Requisiciones',
    canMatch: [REQUISICIONES],
    loadComponent: () => import('./requisiciones/requisiciones').then((m) => m.Requisiciones),
  },
  {
    path: 'nueva',
    title: 'Nueva requisición',
    canMatch: [SOLICITANTES],
    loadComponent: () =>
      import('./requisicion-form/requisicion-form').then((m) => m.RequisicionForm),
  },
  {
    path: ':id/editar',
    title: 'Editar requisición',
    canMatch: [SOLICITANTES],
    loadComponent: () =>
      import('./requisicion-form/requisicion-form').then((m) => m.RequisicionForm),
  },
  {
    path: ':id',
    title: 'Requisición',
    canMatch: [REQUISICIONES],
    loadComponent: () =>
      import('./requisicion-detalle/requisicion-detalle').then((m) => m.RequisicionDetalleVista),
  },
];

export const ORDENES_COMPRA_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Órdenes de compra',
    canMatch: [ORDENES],
    loadComponent: () => import('./ordenes/ordenes').then((m) => m.Ordenes),
  },
  {
    path: 'nueva',
    title: 'Nueva orden de compra',
    canMatch: [COMPRADORES],
    loadComponent: () => import('./orden-form/orden-form').then((m) => m.OrdenForm),
  },
  {
    path: ':id/editar',
    title: 'Editar orden de compra',
    canMatch: [COMPRADORES],
    loadComponent: () => import('./orden-form/orden-form').then((m) => m.OrdenForm),
  },
  {
    path: ':id',
    title: 'Orden de compra',
    canMatch: [ORDENES],
    loadComponent: () => import('./orden-detalle/orden-detalle').then((m) => m.OrdenDetalleVista),
  },
];

export const RECEPCIONES_ROUTES: Routes = [
  {
    path: '',
    title: 'Recepciones y devoluciones',
    canMatch: [ORDENES],
    loadComponent: () => import('./recepciones/recepciones').then((m) => m.Recepciones),
  },
];

export const CUENTAS_POR_PAGAR_ROUTES: Routes = [
  {
    path: '',
    title: 'Cuentas por pagar',
    canMatch: [FINANCIERO],
    loadComponent: () =>
      import('./cuentas-por-pagar/cuentas-por-pagar').then((m) => m.CuentasPorPagar),
  },
];

export const REPORTE_COMPRAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Compras por proveedor',
    canMatch: [FINANCIERO],
    loadComponent: () => import('./reporte-compras/reporte-compras').then((m) => m.ReporteCompras),
  },
];
