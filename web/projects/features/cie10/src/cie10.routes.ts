import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const CIE10_ROUTES: Routes = [
  {
    path: '',
    title: 'Catálogo CIE-10',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () => import('./catalogo/catalogo').then((m) => m.CatalogoCie10),
  },
];
