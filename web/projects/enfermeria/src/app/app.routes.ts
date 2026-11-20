import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { AuthService, entradaModulo, roleGuard } from '@tusalud/core';
import { APP } from './app.id';

export const routes: Routes = [
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado',
    loadComponent: () => import('@tusalud/ui/forbidden').then((m) => m.Forbidden),
  },
  {
    path: '',
    canActivate: [entradaModulo(APP)],
    canActivateChild: [entradaModulo(APP)],
    loadComponent: () => import('@tusalud/ui/app-shell').then((m) => m.AppShell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => inject(AuthService).homeUrl(),
      },
      {
        path: 'inicio',
        title: 'Inicio',
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioEnfermeria),
      },
      {
        path: 'hospitalizacion',
        loadChildren: () =>
          import('@tusalud/features/hospitalizacion').then((m) => m.HOSPITALIZACION_ROUTES),
      },
      {
        path: 'vacunacion',
        canMatch: [roleGuard('ROLE_ENFERMERIA')],
        loadChildren: () =>
          import('@tusalud/features/pediatria').then((m) => m.VACUNACION_CLINICA_ROUTES),
      },
      {
        path: 'crecimiento',
        canMatch: [roleGuard('ROLE_ENFERMERIA')],
        loadChildren: () =>
          import('@tusalud/features/pediatria').then((m) => m.CRECIMIENTO_CLINICA_ROUTES),
      },
      {
        path: 'calendario',
        loadChildren: () => import('@tusalud/features/calendario').then((m) => m.CALENDARIO_ROUTES),
      },
      {
        path: 'mis-boletas',
        loadChildren: () => import('@tusalud/features/planilla').then((m) => m.MIS_BOLETAS_ROUTES),
      },
      {
        path: 'mi-asistencia',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.MI_ASISTENCIA_ROUTES),
      },
      {
        path: 'mi-espacio',
        loadChildren: () => import('@tusalud/features/rrhh').then((m) => m.MI_ESPACIO_ROUTES),
      },
      {
        path: 'ausencias',
        redirectTo: 'mi-espacio/ausencias',
      },
      {
        path: 'notificaciones',
        loadChildren: () =>
          import('@tusalud/features/notificaciones').then((m) => m.NOTIFICACIONES_ROUTES),
      },
      {
        path: 'quirofano',
        canMatch: [roleGuard('ROLE_ENFERMERIA')],
        loadChildren: () => import('@tusalud/features/quirofano').then((m) => m.QUIROFANO_ROUTES),
      },
      {
        path: 'esterilizacion',
        canMatch: [roleGuard('ROLE_ENFERMERIA')],
        loadChildren: () =>
          import('@tusalud/features/esterilizacion').then((m) => m.ESTERILIZACION_ROUTES),
      },
      {
        path: 'perfil',
        loadChildren: () => import('@tusalud/features/perfil').then((m) => m.PERFIL_ROUTES),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('@tusalud/ui/not-found').then((m) => m.NotFound),
  },
];
