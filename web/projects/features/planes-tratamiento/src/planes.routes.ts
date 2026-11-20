import { Routes } from '@angular/router';

export const PACIENTES_CRONICOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Pacientes crónicos',
    loadComponent: () =>
      import('./pacientes-cronicos/pacientes-cronicos').then((m) => m.PacientesCronicos),
  },
];

export const MI_PLAN_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi plan de tratamiento',
    loadComponent: () => import('./mi-plan/mi-plan').then((m) => m.MiPlan),
  },
];
