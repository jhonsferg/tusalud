import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const EJECUTIVO = roleGuard('ROLE_GERENCIA', 'ROLE_ADMIN');
const FINANCIERO = roleGuard('ROLE_FINANZAS', 'ROLE_GERENCIA', 'ROLE_ADMIN');
const OPERATIVO = roleGuard('ROLE_OPERACIONES', 'ROLE_GERENCIA', 'ROLE_ADMIN');
const CALIDAD = roleGuard('ROLE_CALIDAD', 'ROLE_GERENCIA', 'ROLE_ADMIN');
const MORBILIDAD = roleGuard('ROLE_CALIDAD', 'ROLE_OPERACIONES', 'ROLE_GERENCIA', 'ROLE_ADMIN');

export const REPORTES_ROUTES: Routes = [
  {
    path: 'ejecutivo',
    title: 'Reporte ejecutivo',
    canMatch: [EJECUTIVO],
    loadComponent: () => import('./ejecutivo/ejecutivo').then((m) => m.ReporteEjecutivo),
  },
  {
    path: 'financiero',
    title: 'Reporte financiero',
    canMatch: [FINANCIERO],
    loadComponent: () => import('./financiero/financiero').then((m) => m.ReporteFinanciero),
  },
  {
    path: 'operativo',
    title: 'Reporte operativo',
    canMatch: [OPERATIVO],
    loadComponent: () => import('./operativo/operativo').then((m) => m.ReporteOperativo),
  },
  {
    path: 'calidad',
    title: 'Reporte de calidad',
    canMatch: [CALIDAD],
    loadComponent: () => import('./calidad/calidad').then((m) => m.ReporteCalidad),
  },
  {
    path: 'morbilidad',
    title: 'Morbilidad',
    canMatch: [MORBILIDAD],
    loadComponent: () => import('./morbilidad/morbilidad').then((m) => m.ReporteMorbilidad),
  },
];
