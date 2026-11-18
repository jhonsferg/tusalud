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
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioRecepcion),
      },
      {
        path: 'recepcion',
        loadChildren: () => import('@tusalud/features/recepcion').then((m) => m.RECEPCION_ROUTES),
      },
      {
        path: 'imagenes',
        loadChildren: () => import('@tusalud/features/imagenes').then((m) => m.IMAGENES_ROUTES),
      },
      {
        path: 'hospitalizacion',
        loadChildren: () =>
          import('@tusalud/features/hospitalizacion').then((m) => m.HOSPITALIZACION_ROUTES),
      },
      {
        path: 'seguros',
        canMatch: [roleGuard('ROLE_RECEPCION', 'ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/seguros').then((m) => m.SEGUROS_RECEPCION_ROUTES),
      },
      {
        path: 'preautorizaciones',
        redirectTo: 'seguros/preautorizaciones',
      },
      {
        path: 'calendario',
        loadChildren: () => import('@tusalud/features/calendario').then((m) => m.CALENDARIO_ROUTES),
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
