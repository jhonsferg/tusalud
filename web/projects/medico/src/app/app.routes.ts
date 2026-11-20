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
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioMedico),
      },
      {
        path: 'agenda',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () => import('@tusalud/features/agenda').then((m) => m.AGENDA_ROUTES),
      },
      {
        path: 'citas',
        loadChildren: () => import('@tusalud/features/citas').then((m) => m.CITAS_ROUTES),
      },
      {
        path: 'pacientes',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () =>
          import('@tusalud/features/historia-clinica').then((m) => m.PACIENTES_ROUTES),
      },
      {
        path: 'cola',
        loadChildren: () => import('@tusalud/features/recepcion').then((m) => m.COLA_ROUTES),
      },
      {
        path: 'laboratorio',
        loadChildren: () =>
          import('@tusalud/features/laboratorio').then((m) => m.LABORATORIO_ROUTES),
      },
      {
        path: 'teleconsultas',
        loadChildren: () =>
          import('@tusalud/features/telemedicina').then((m) => m.TELECONSULTAS_ROUTES),
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
        path: 'preautorizaciones',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () =>
          import('@tusalud/features/seguros').then((m) => m.SEGUROS_MEDICO_ROUTES),
      },
      {
        path: 'documentos',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () =>
          import('@tusalud/features/documentos-medicos').then((m) => m.DOCUMENTOS_MEDICO_ROUTES),
      },
      {
        path: 'consentimientos',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () =>
          import('@tusalud/features/documentos-medicos').then(
            (m) => m.CONSENTIMIENTOS_MEDICO_ROUTES,
          ),
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
