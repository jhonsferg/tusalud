import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const CITAS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Mis citas',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./mis-citas/mis-citas').then((m) => m.MisCitas),
  },
  {
    path: 'nueva',
    title: 'Reservar cita',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./cita-form/cita-form').then((m) => m.CitaForm),
  },
  {
    path: ':id/reprogramar',
    title: 'Reprogramar cita',
    canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
    loadComponent: () =>
      import('./cita-reprogramar/cita-reprogramar').then((m) => m.CitaReprogramar),
  },
  {
    path: ':id/atencion',
    title: 'Atención médica',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () => import('./atencion-form/atencion-form').then((m) => m.AtencionForm),
  },
  {
    path: ':id',
    title: 'Detalle de la cita',
    canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE')],
    loadComponent: () => import('./cita-detail/cita-detail').then((m) => m.CitaDetail),
  },
];
