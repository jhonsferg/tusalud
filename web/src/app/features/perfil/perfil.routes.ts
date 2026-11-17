import { Routes } from '@angular/router';

export const PERFIL_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi perfil',
    loadComponent: () => import('@tusalud/app/features/perfil/perfil/perfil').then((m) => m.Perfil),
  },
];
