import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const CAJERO = roleGuard('ROLE_CAJA');
const LECTURA = roleGuard('ROLE_CAJA', 'ROLE_ADMIN');

export const CAJA_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Caja',
    canMatch: [CAJERO],
    loadComponent: () => import('./caja-panel/caja-panel').then((m) => m.CajaPanel),
  },
  {
    path: 'cobrar',
    title: 'Cobrar',
    canMatch: [CAJERO],
    loadComponent: () => import('./cobrar/cobrar').then((m) => m.Cobrar),
  },
  {
    path: 'comprobantes',
    title: 'Comprobantes',
    canMatch: [LECTURA],
    loadComponent: () => import('./comprobantes/comprobantes').then((m) => m.Comprobantes),
  },
  {
    path: 'historial',
    title: 'Historial de caja',
    canMatch: [LECTURA],
    loadComponent: () => import('./historial-caja/historial-caja').then((m) => m.HistorialCaja),
  },
  {
    path: 'reportes',
    title: 'Reporte de ingresos',
    canMatch: [LECTURA],
    loadComponent: () => import('./reportes/reportes').then((m) => m.ReportesCaja),
  },
];
