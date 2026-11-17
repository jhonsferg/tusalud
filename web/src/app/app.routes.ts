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
        canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/app/features/especialidades/especialidades.routes').then(
            (m) => m.ESPECIALIDADES_ROUTES,
          ),
      },
      {
        path: 'medicos',
        canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/app/features/medicos/medicos.routes').then((m) => m.MEDICOS_ROUTES),
      },
      {
        path: 'citas',
        loadChildren: () =>
          import('@tusalud/app/features/citas/citas.routes').then((m) => m.CITAS_ROUTES),
      },
      {
        path: 'pagos',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/app/features/pagos/pagos.routes').then((m) => m.PAGOS_ROUTES),
      },
      {
        path: 'historia-clinica',
        canMatch: [roleGuard('ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/app/features/historia-clinica/historia-clinica.routes').then(
            (m) => m.HISTORIA_CLINICA_ROUTES,
          ),
      },
      {
        path: 'pacientes',
        canMatch: [roleGuard('ROLE_MEDICO', 'ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/app/features/historia-clinica/historia-clinica.routes').then(
            (m) => m.PACIENTES_ROUTES,
          ),
      },
      {
        path: 'calendario',
        loadChildren: () =>
          import('@tusalud/app/features/calendario/calendario.routes').then(
            (m) => m.CALENDARIO_ROUTES,
          ),
      },
      {
        path: 'almacen',
        loadChildren: () =>
          import('@tusalud/app/features/almacen/almacen.routes').then((m) => m.ALMACEN_ROUTES),
      },
      {
        path: 'farmacia',
        loadChildren: () =>
          import('@tusalud/app/features/farmacia/farmacia.routes').then((m) => m.FARMACIA_ROUTES),
      },
      {
        path: 'mis-medicamentos',
        loadChildren: () =>
          import('@tusalud/app/features/farmacia/farmacia.routes').then(
            (m) => m.MEDICAMENTOS_ROUTES,
          ),
      },
      {
        path: 'laboratorio',
        loadChildren: () =>
          import('@tusalud/app/features/laboratorio/laboratorio.routes').then(
            (m) => m.LABORATORIO_ROUTES,
          ),
      },
      {
        path: 'mis-resultados',
        loadChildren: () =>
          import('@tusalud/app/features/laboratorio/laboratorio.routes').then(
            (m) => m.RESULTADOS_ROUTES,
          ),
      },
      {
        path: 'recepcion',
        loadChildren: () =>
          import('@tusalud/app/features/recepcion/recepcion.routes').then(
            (m) => m.RECEPCION_ROUTES,
          ),
      },
      {
        path: 'cola',
        loadChildren: () =>
          import('@tusalud/app/features/recepcion/recepcion.routes').then((m) => m.COLA_ROUTES),
      },
      {
        path: 'caja',
        loadChildren: () =>
          import('@tusalud/app/features/caja/caja.routes').then((m) => m.CAJA_ROUTES),
      },
      {
        path: 'hospitalizacion',
        loadChildren: () =>
          import('@tusalud/app/features/hospitalizacion/hospitalizacion.routes').then(
            (m) => m.HOSPITALIZACION_ROUTES,
          ),
      },
      {
        path: 'mis-ingresos',
        loadChildren: () =>
          import('@tusalud/app/features/hospitalizacion/hospitalizacion.routes').then(
            (m) => m.MIS_INGRESOS_ROUTES,
          ),
      },
      {
        path: 'notificaciones',
        loadChildren: () =>
          import('@tusalud/app/features/notificaciones/notificaciones.routes').then(
            (m) => m.NOTIFICACIONES_ROUTES,
          ),
      },
      {
        path: 'perfil',
        loadChildren: () =>
          import('@tusalud/app/features/perfil/perfil.routes').then((m) => m.PERFIL_ROUTES),
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
