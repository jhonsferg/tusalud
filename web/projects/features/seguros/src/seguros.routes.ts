import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const CATALOGO = roleGuard('ROLE_FINANZAS', 'ROLE_ADMIN', 'ROLE_GERENCIA', 'ROLE_RECEPCION');
const CARTERA = roleGuard('ROLE_FINANZAS', 'ROLE_GERENCIA', 'ROLE_ADMIN');
const PREAUTORIZACIONES = roleGuard('ROLE_FINANZAS', 'ROLE_RECEPCION', 'ROLE_ADMIN');
const POLIZAS = roleGuard('ROLE_RECEPCION', 'ROLE_ADMIN');

export const SEGUROS_FINANZAS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'cartera' },
  {
    path: 'aseguradoras',
    title: 'Aseguradoras y convenios',
    canMatch: [CATALOGO],
    loadComponent: () => import('./catalogo/catalogo').then((m) => m.Catalogo),
  },
  {
    path: 'cartera',
    title: 'Cartera de aseguradoras',
    canMatch: [CARTERA],
    loadComponent: () => import('./cartera/cartera').then((m) => m.Cartera),
  },
  {
    path: 'liquidaciones',
    title: 'Liquidaciones',
    canMatch: [CARTERA],
    loadComponent: () => import('./liquidaciones/liquidaciones').then((m) => m.Liquidaciones),
  },
  {
    path: 'preautorizaciones',
    title: 'Preautorizaciones',
    canMatch: [PREAUTORIZACIONES],
    loadComponent: () =>
      import('./preautorizaciones/preautorizaciones').then((m) => m.PreautorizacionesGestion),
  },
];

export const SEGUROS_RECEPCION_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'polizas' },
  {
    path: 'polizas',
    title: 'Pólizas de pacientes',
    canMatch: [POLIZAS],
    loadComponent: () => import('./polizas/polizas').then((m) => m.Polizas),
  },
  {
    path: 'preautorizaciones',
    title: 'Preautorizaciones',
    canMatch: [PREAUTORIZACIONES],
    loadComponent: () =>
      import('./preautorizaciones/preautorizaciones').then((m) => m.PreautorizacionesGestion),
  },
  {
    path: 'aseguradoras',
    title: 'Aseguradoras y convenios',
    canMatch: [CATALOGO],
    loadComponent: () => import('./catalogo/catalogo').then((m) => m.Catalogo),
  },
];

export const SEGUROS_ADMIN_ROUTES: Routes = SEGUROS_FINANZAS_ROUTES;

export const SEGUROS_GERENCIA_ROUTES: Routes = SEGUROS_FINANZAS_ROUTES;

export const SEGUROS_MEDICO_ROUTES: Routes = [
  {
    path: '',
    title: 'Preautorizaciones',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () =>
      import('./mis-preautorizaciones/mis-preautorizaciones').then((m) => m.MisPreautorizaciones),
  },
];

export const SEGUROS_PACIENTE_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis seguros',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./mis-seguros/mis-seguros').then((m) => m.MisSeguros),
  },
];
