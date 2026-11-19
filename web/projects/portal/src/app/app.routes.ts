import { Routes } from '@angular/router';

import { guestGuard } from '@tusalud/core';
import { raizPortal } from './raiz.guard';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadComponent: () => import('@tusalud/ui/app-shell').then((m) => m.AuthLayout),
    loadChildren: () => import('./auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'verificar',
    loadChildren: () =>
      import('@tusalud/features/documentos-medicos').then((m) => m.VERIFICACION_PUBLICA_ROUTES),
  },
  {
    path: 'baja',
    loadChildren: () => import('@tusalud/features/canales').then((m) => m.BAJA_CANAL_ROUTES),
  },
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado',
    loadComponent: () => import('@tusalud/ui/forbidden').then((m) => m.Forbidden),
  },
  { path: '', pathMatch: 'full', canActivate: [raizPortal], children: [] },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('@tusalud/ui/not-found').then((m) => m.NotFound),
  },
];
