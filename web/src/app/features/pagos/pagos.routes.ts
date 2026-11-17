import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const PAGOS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Mis pagos',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./mis-pagos/mis-pagos').then((m) => m.MisPagos),
  },
  {
    path: 'metodos',
    title: 'Métodos de pago',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./metodos-pago/metodos-pago').then((m) => m.MetodosPago),
  },
];
