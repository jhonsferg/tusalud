import { Routes } from '@angular/router';

export const PERFIL_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi perfil',
    loadComponent: () => import('./perfil/perfil').then((m) => m.Perfil),
  },
];
