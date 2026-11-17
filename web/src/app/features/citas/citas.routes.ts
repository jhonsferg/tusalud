import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/app/core/guards/role.guard';

export const CITAS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Mis citas',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('@tusalud/app/features/citas/mis-citas/mis-citas').then((m) => m.MisCitas),
  },
  {
    path: 'nueva',
    title: 'Reservar cita',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('@tusalud/app/features/citas/cita-form/cita-form').then((m) => m.CitaForm),
  },
  {
    path: ':id/atencion',
    title: 'Atención médica',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () =>
      import('@tusalud/app/features/citas/atencion-form/atencion-form').then((m) => m.AtencionForm),
  },
  {
    path: ':id',
    title: 'Detalle de la cita',
    canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE')],
    loadComponent: () =>
      import('@tusalud/app/features/citas/cita-detail/cita-detail').then((m) => m.CitaDetail),
  },
];
