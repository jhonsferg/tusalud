import { Routes } from '@angular/router';
// @tusalud/app/features/medicos/
import { roleGuard } from '@tusalud/app/core/guards/role.guard';

export const MEDICOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Médicos',
    loadComponent: () =>
      import('@tusalud/app/features/medicos/medico-list/medico-list').then((m) => m.MedicoList),
  },
  {
    path: 'nuevo',
    title: 'Nuevo médico',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () =>
      import('@tusalud/app/features/medicos/medico-form/medico-form').then((m) => m.MedicoForm),
  },
  {
    path: ':id',
    title: 'Detalle del médico',
    loadComponent: () =>
      import('@tusalud/app/features/medicos/medico-detail/medico-detail').then(
        (m) => m.MedicoDetail,
      ),
  },
  {
    path: ':id/horarios',
    title: 'Horarios de atención',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () =>
      import('@tusalud/app/features/medicos/medico-horarios/medico-horarios').then(
        (m) => m.MedicoHorarios,
      ),
  },
  {
    path: ':id/disponibilidad',
    title: 'Disponibilidad',
    loadComponent: () =>
      import('@tusalud/app/features/medicos/medico-disponibilidad/medico-disponibilidad').then(
        (m) => m.MedicoDisponibilidad,
      ),
  },
];
