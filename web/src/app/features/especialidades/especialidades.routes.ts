import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/app/core/guards/role.guard';

export const ESPECIALIDADES_ROUTES: Routes = [
  {
    path: '',
    title: 'Especialidades',
    loadComponent: () =>
      import('@tusalud/app/features/especialidades/especialidad-list/especialidad-list').then(
        (m) => m.EspecialidadList,
      ),
  },
  {
    path: 'nueva',
    title: 'Nueva especialidad',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () =>
      import('@tusalud/app/features/especialidades/especialidad-form/especialidad-form').then(
        (m) => m.EspecialidadForm,
      ),
  },
  {
    path: ':id/editar',
    title: 'Editar especialidad',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () =>
      import('@tusalud/app/features/especialidades/especialidad-form/especialidad-form').then(
        (m) => m.EspecialidadForm,
      ),
  },
];
