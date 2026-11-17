import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/app/core/guards/role.guard';

export const PAGOS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Mis pagos',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('@tusalud/app/features/pagos/mis-pagos/mis-pagos').then((m) => m.MisPagos),
  },
  {
    path: 'metodos',
    title: 'Métodos de pago',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('@tusalud/app/features/pagos/metodos-pago/metodos-pago').then((m) => m.MetodosPago),
  },
];
