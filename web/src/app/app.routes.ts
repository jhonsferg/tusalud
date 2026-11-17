import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { authGuard, AuthService, guestGuard, LOGIN_URL, roleGuard } from '@tusalud/core';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadComponent: () => import('./layout/auth-layout/auth-layout').then((m) => m.AuthLayout),
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
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
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
          import('./features/especialidades/especialidades.routes').then(
            (m) => m.ESPECIALIDADES_ROUTES,
          ),
      },
      {
        path: 'medicos',
        canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
        loadChildren: () =>
          import('./features/medicos/medicos.routes').then((m) => m.MEDICOS_ROUTES),
      },
      {
        path: 'citas',
        loadChildren: () => import('./features/citas/citas.routes').then((m) => m.CITAS_ROUTES),
      },
      {
        path: 'pagos',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () => import('./features/pagos/pagos.routes').then((m) => m.PAGOS_ROUTES),
      },
      {
        path: 'historia-clinica',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('./features/historia-clinica/historia-clinica.routes').then(
            (m) => m.HISTORIA_CLINICA_ROUTES,
          ),
      },
      {
        path: 'pacientes',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadChildren: () =>
          import('./features/historia-clinica/historia-clinica.routes').then(
            (m) => m.PACIENTES_ROUTES,
          ),
      },
      {
        path: 'calendario',
        loadChildren: () =>
          import('./features/calendario/calendario.routes').then((m) => m.CALENDARIO_ROUTES),
      },
      {
        path: 'almacen',
        loadChildren: () =>
          import('./features/almacen/almacen.routes').then((m) => m.ALMACEN_ROUTES),
      },
      {
        path: 'farmacia',
        loadChildren: () =>
          import('./features/farmacia/farmacia.routes').then((m) => m.FARMACIA_ROUTES),
      },
      {
        path: 'mis-medicamentos',
        loadChildren: () =>
          import('./features/farmacia/farmacia.routes').then((m) => m.MEDICAMENTOS_ROUTES),
      },
      {
        path: 'laboratorio',
        loadChildren: () =>
          import('./features/laboratorio/laboratorio.routes').then((m) => m.LABORATORIO_ROUTES),
      },
      {
        path: 'mis-resultados',
        loadChildren: () =>
          import('./features/laboratorio/laboratorio.routes').then((m) => m.RESULTADOS_ROUTES),
      },
      {
        path: 'recepcion',
        loadChildren: () =>
          import('./features/recepcion/recepcion.routes').then((m) => m.RECEPCION_ROUTES),
      },
      {
        path: 'cola',
        loadChildren: () =>
          import('./features/recepcion/recepcion.routes').then((m) => m.COLA_ROUTES),
      },
      {
        path: 'caja',
        loadChildren: () => import('./features/caja/caja.routes').then((m) => m.CAJA_ROUTES),
      },
      {
        path: 'hospitalizacion',
        loadChildren: () =>
          import('./features/hospitalizacion/hospitalizacion.routes').then(
            (m) => m.HOSPITALIZACION_ROUTES,
          ),
      },
      {
        path: 'mis-ingresos',
        loadChildren: () =>
          import('./features/hospitalizacion/hospitalizacion.routes').then(
            (m) => m.MIS_INGRESOS_ROUTES,
          ),
      },
      {
        path: 'notificaciones',
        loadChildren: () =>
          import('./features/notificaciones/notificaciones.routes').then(
            (m) => m.NOTIFICACIONES_ROUTES,
          ),
      },
      {
        path: 'perfil',
        loadChildren: () => import('./features/perfil/perfil.routes').then((m) => m.PERFIL_ROUTES),
      },
      {
        path: 'agenda',
        title: 'Mi agenda',
        canMatch: [roleGuard('ROLE_MEDICO')],
        loadComponent: () => import('./features/agenda/agenda/agenda').then((m) => m.Agenda),
      },
      {
        path: 'admin',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('@tusalud/ui/not-found').then((m) => m.NotFound),
  },
];
