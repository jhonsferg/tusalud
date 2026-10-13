import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { authGuard } from '@tusalud/app/core/guards/auth.guard';
import { guestGuard } from '@tusalud/app/core/guards/guest.guard';
import { roleGuard } from '@tusalud/app/core/guards/role.guard';
import { AuthService, LOGIN_URL } from '@tusalud/app/core/services/auth.service';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('@tusalud/app/layout/auth-layout/auth-layout').then((m) => m.AuthLayout),
    loadChildren: () => import('@tusalud/app/features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado',
    loadComponent: () =>
      import('@tusalud/app/shared/components/forbidden/forbidden').then((m) => m.Forbidden),
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () => import('@tusalud/app/layout/shell/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => {
          const auth = inject(AuthService);
          return auth.isAuthenticated() ? auth.homeUrl() : LOGIN_URL;
        },
      },
      {
        path: 'especialidades',
        loadChildren: () =>
          import('@tusalud/app/features/especialidades/especialidades.routes').then(
            (m) => m.ESPECIALIDADES_ROUTES,
          ),
      },
      {
        path: 'medicos',
        loadChildren: () =>
          import('@tusalud/app/features/medicos/medicos.routes').then((m) => m.MEDICOS_ROUTES),
      },
      {
        path: 'citas',
        loadChildren: () =>
          import('@tusalud/app/features/citas/citas.routes').then((m) => m.CITAS_ROUTES),
      },
      {
        path: 'agenda',
        title: 'Mi agenda',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadComponent: () =>
          import('@tusalud/app/features/agenda/agenda/agenda').then((m) => m.Agenda),
      },
      {
        path: 'admin',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/app/features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () =>
      import('@tusalud/app/shared/components/not-found/not-found').then((m) => m.NotFound),
  },
];
