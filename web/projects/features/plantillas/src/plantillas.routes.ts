import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const PLANTILLAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Plantillas de documentos',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () => import('./plantillas/plantillas').then((m) => m.Plantillas),
  },
];
