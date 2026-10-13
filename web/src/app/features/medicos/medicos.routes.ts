import { Routes } from '@angular/router';

import { roleGuard } from '../../core/guards/role.guard';

export const MEDICOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Médicos',
    loadComponent: () => import('./medico-list/medico-list').then((m) => m.MedicoList),
  },
  {
    path: 'nuevo',
    title: 'Nuevo médico',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () => import('./medico-form/medico-form').then((m) => m.MedicoForm),
  },
  {
    path: ':id',
    title: 'Detalle del médico',
    loadComponent: () => import('./medico-detail/medico-detail').then((m) => m.MedicoDetail),
  },
  {
    path: ':id/horarios',
    title: 'Horarios de atención',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () => import('./medico-horarios/medico-horarios').then((m) => m.MedicoHorarios),
  },
  {
    path: ':id/disponibilidad',
    title: 'Disponibilidad',
    loadComponent: () =>
      import('./medico-disponibilidad/medico-disponibilidad').then((m) => m.MedicoDisponibilidad),
  },
];
