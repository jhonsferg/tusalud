import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const CLINICO = roleGuard('ROLE_ENFERMERIA', 'ROLE_MEDICO', 'ROLE_ADMIN', 'ROLE_RECEPCION');
const OPERA = roleGuard('ROLE_ENFERMERIA', 'ROLE_MEDICO', 'ROLE_ADMIN');

export const HOSPITALIZACION_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Hospitalización',
    canMatch: [CLINICO],
    loadComponent: () => import('./hosp-panel/hosp-panel').then((m) => m.HospPanel),
  },
  {
    path: 'emergencias',
    title: 'Emergencias',
    canMatch: [CLINICO],
    loadComponent: () => import('./emergencias/emergencias').then((m) => m.Emergencias),
  },
  {
    path: 'ingresos',
    pathMatch: 'full',
    title: 'Ingresos',
    canMatch: [CLINICO],
    loadComponent: () => import('./ingresos/ingresos-hosp').then((m) => m.IngresosHosp),
  },
  {
    path: 'ingresos/:id',
    title: 'Ingreso hospitalario',
    canMatch: [CLINICO],
    loadComponent: () => import('./ingreso-detalle/ingreso-detalle').then((m) => m.IngresoDetalle),
  },
  {
    path: 'camas',
    title: 'Camas',
    canMatch: [OPERA],
    loadComponent: () => import('./camas/camas').then((m) => m.Camas),
  },
];

export const MIS_INGRESOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis hospitalizaciones',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./mis-ingresos/mis-ingresos').then((m) => m.MisIngresos),
  },
];
