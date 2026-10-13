import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    title: 'Iniciar sesión',
    loadComponent: () => import('@tusalud/app/features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'registro',
    title: 'Crear cuenta',
    loadComponent: () =>
      import('@tusalud/app/features/auth/register/register').then((m) => m.Register),
  },
];
