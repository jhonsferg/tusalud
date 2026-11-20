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
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioPaciente),
      },
      {
        path: 'citas',
        loadChildren: () => import('@tusalud/features/citas').then((m) => m.CITAS_ROUTES),
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
        path: 'mis-medicamentos',
        loadChildren: () => import('@tusalud/features/farmacia').then((m) => m.MEDICAMENTOS_ROUTES),
      },
      {
        path: 'mis-resultados',
        loadChildren: () =>
          import('@tusalud/features/laboratorio').then((m) => m.RESULTADOS_ROUTES),
      },
      {
        path: 'teleconsultas',
        loadChildren: () =>
          import('@tusalud/features/telemedicina').then((m) => m.TELECONSULTAS_ROUTES),
      },
      {
        path: 'mis-estudios',
        loadChildren: () => import('@tusalud/features/imagenes').then((m) => m.ESTUDIOS_ROUTES),
      },
      {
        path: 'mis-ingresos',
        loadChildren: () =>
          import('@tusalud/features/hospitalizacion').then((m) => m.MIS_INGRESOS_ROUTES),
      },
      {
        path: 'mis-seguros',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/seguros').then((m) => m.SEGUROS_PACIENTE_ROUTES),
      },
      {
        path: 'mis-documentos',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/documentos-medicos').then((m) => m.DOCUMENTOS_PACIENTE_ROUTES),
      },
      {
        path: 'mis-consentimientos',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/documentos-medicos').then(
            (m) => m.CONSENTIMIENTOS_PACIENTE_ROUTES,
          ),
      },
      {
        path: 'mi-vacunacion',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/pediatria').then((m) => m.MI_VACUNACION_ROUTES),
      },
      {
        path: 'mi-crecimiento',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/pediatria').then((m) => m.MI_CRECIMIENTO_ROUTES),
      },
      {
        path: 'mi-salud-dental',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/odontologia').then((m) => m.MI_SALUD_DENTAL_ROUTES),
      },
      {
        path: 'mis-comprobantes',
        loadChildren: () =>
          import('@tusalud/features/facturacion').then((m) => m.MIS_COMPROBANTES_ROUTES),
      },
      {
        path: 'privacidad',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.MI_PRIVACIDAD_ROUTES),
      },
      {
        path: 'calendario',
        loadChildren: () => import('@tusalud/features/calendario').then((m) => m.CALENDARIO_ROUTES),
      },
      {
        path: 'notificaciones',
        loadChildren: () =>
          import('@tusalud/features/notificaciones').then((m) => m.NOTIFICACIONES_ROUTES),
      },
      {
        path: 'mis-cirugias',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/quirofano').then((m) => m.MIS_CIRUGIAS_ROUTES),
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
