import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const GESTION = roleGuard('ROLE_FARMACIA', 'ROLE_ADMIN');
const DISPENSACION = roleGuard('ROLE_FARMACIA');

export const FARMACIA_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Farmacia',
    canMatch: [DISPENSACION],
    loadComponent: () => import('./farmacia-panel/farmacia-panel').then((m) => m.FarmaciaPanel),
  },
  {
    path: 'dispensar',
    title: 'Dispensar',
    canMatch: [DISPENSACION],
    loadComponent: () => import('./dispensar/dispensar').then((m) => m.Dispensar),
  },
  {
    path: 'historial',
    title: 'Historial de dispensaciones',
    canMatch: [GESTION],
    loadComponent: () => import('./historial/historial').then((m) => m.HistorialFarmacia),
  },
];

export const MEDICAMENTOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis medicamentos',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('./mis-medicamentos/mis-medicamentos').then((m) => m.MisMedicamentos),
  },
];
