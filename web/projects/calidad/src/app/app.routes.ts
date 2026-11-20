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
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioCalidad),
      },
      {
        path: 'reportes',
        canMatch: [roleGuard('ROLE_CALIDAD')],
        loadChildren: () => import('@tusalud/features/reportes').then((m) => m.REPORTES_ROUTES),
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
        path: 'privacidad/solicitudes',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.SOLICITUDES_ARCO_ROUTES),
      },
      {
        path: 'privacidad/registro',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.REGISTRO_TRATAMIENTOS_ROUTES),
      },
      {
        path: 'privacidad/retencion',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.RETENCION_DATOS_ROUTES),
      },
      {
        path: 'privacidad/incidentes',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.INCIDENTES_DATOS_ROUTES),
      },
      {
        path: 'privacidad/politica',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.POLITICA_ADMIN_ROUTES),
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
