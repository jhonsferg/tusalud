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
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioRrhh),
      },
      {
        path: 'rrhh',
        canMatch: [roleGuard('ROLE_RRHH')],
        loadChildren: () => import('@tusalud/features/rrhh').then((m) => m.RRHH_ROUTES),
      },
      {
        path: 'planilla',
        loadChildren: () => import('@tusalud/features/planilla').then((m) => m.PLANILLAS_ROUTES),
      },
      {
        path: 'contratos-laborales',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.CONTRATOS_LABORALES_ROUTES),
      },
      {
        path: 'conceptos-planilla',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.CONCEPTOS_PLANILLA_ROUTES),
      },
      {
        path: 'parametros-planilla',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.PARAMETROS_PLANILLA_ROUTES),
      },
      {
        path: 'asistencia',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.ASISTENCIA_PERSONAL_ROUTES),
      },
      {
        path: 'terminales-asistencia',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.TERMINALES_ASISTENCIA_ROUTES),
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
        redirectTo: '/rrhh/ausencias',
      },
      {
        path: 'calendario',
        loadChildren: () => import('@tusalud/features/calendario').then((m) => m.CALENDARIO_ROUTES),
      },
      {
        path: 'mis-exportaciones',
        loadChildren: () =>
          import('@tusalud/features/exportaciones').then((m) => m.EXPORTACIONES_ROUTES),
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
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('@tusalud/ui/not-found').then((m) => m.NotFound),
  },
];
