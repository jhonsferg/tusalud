import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const RESPALDOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Respaldos y recuperación',
    canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_OPERACIONES')],
    loadComponent: () => import('./respaldos/respaldos').then((m) => m.Respaldos),
  },
];
