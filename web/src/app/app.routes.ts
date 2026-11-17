import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { authGuard, AuthService, guestGuard, LOGIN_URL, roleGuard } from '@tusalud/core';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadComponent: () => import('@tusalud/ui/app-shell').then((m) => m.AuthLayout),
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado',
    loadComponent: () => import('@tusalud/ui/forbidden').then((m) => m.Forbidden),
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () => import('@tusalud/ui/app-shell').then((m) => m.AppShell),
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
        canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/especialidades').then((m) => m.ESPECIALIDADES_ROUTES),
      },
      {
        path: 'medicos',
        canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
        loadChildren: () => import('@tusalud/features/medicos').then((m) => m.MEDICOS_ROUTES),
      },
      {
        path: 'citas',
        loadChildren: () => import('@tusalud/features/citas').then((m) => m.CITAS_ROUTES),
      },
      {
        path: 'pagos',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () => import('@tusalud/features/pagos').then((m) => m.PAGOS_ROUTES),
      },
      {
        path: 'historia-clinica',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/historia-clinica').then((m) => m.HISTORIA_CLINICA_ROUTES),
      },
      {
        path: 'pacientes',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () =>
          import('@tusalud/features/historia-clinica').then((m) => m.PACIENTES_ROUTES),
      },
      {
        path: 'calendario',
        loadChildren: () => import('@tusalud/features/calendario').then((m) => m.CALENDARIO_ROUTES),
      },
      {
        path: 'almacen',
        loadChildren: () => import('@tusalud/features/almacen').then((m) => m.ALMACEN_ROUTES),
      },
      {
        path: 'farmacia',
        loadChildren: () => import('@tusalud/features/farmacia').then((m) => m.FARMACIA_ROUTES),
      },
      {
        path: 'mis-medicamentos',
        loadChildren: () => import('@tusalud/features/farmacia').then((m) => m.MEDICAMENTOS_ROUTES),
      },
      {
        path: 'laboratorio',
        loadChildren: () =>
          import('@tusalud/features/laboratorio').then((m) => m.LABORATORIO_ROUTES),
      },
      {
        path: 'mis-resultados',
        loadChildren: () =>
          import('@tusalud/features/laboratorio').then((m) => m.RESULTADOS_ROUTES),
      },
      {
        path: 'recepcion',
        loadChildren: () => import('@tusalud/features/recepcion').then((m) => m.RECEPCION_ROUTES),
      },
      {
        path: 'cola',
        loadChildren: () => import('@tusalud/features/recepcion').then((m) => m.COLA_ROUTES),
      },
      {
        path: 'caja',
        loadChildren: () => import('@tusalud/features/caja').then((m) => m.CAJA_ROUTES),
      },
      {
        path: 'hospitalizacion',
        loadChildren: () =>
          import('@tusalud/features/hospitalizacion').then((m) => m.HOSPITALIZACION_ROUTES),
      },
      {
        path: 'mis-ingresos',
        loadChildren: () =>
          import('@tusalud/features/hospitalizacion').then((m) => m.MIS_INGRESOS_ROUTES),
      },
      {
        path: 'notificaciones',
        loadChildren: () =>
          import('@tusalud/features/notificaciones').then((m) => m.NOTIFICACIONES_ROUTES),
      },
      {
        path: 'perfil',
        loadChildren: () => import('@tusalud/features/perfil').then((m) => m.PERFIL_ROUTES),
      },
      {
        path: 'agenda',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () => import('@tusalud/features/agenda').then((m) => m.AGENDA_ROUTES),
      },
      {
        path: 'admin',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/admin').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('@tusalud/ui/not-found').then((m) => m.NotFound),
  },
];
