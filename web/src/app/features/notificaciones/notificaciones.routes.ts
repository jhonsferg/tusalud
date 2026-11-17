import { Routes } from '@angular/router';

export const NOTIFICACIONES_ROUTES: Routes = [
  {
    path: '',
    title: 'Notificaciones',
    loadComponent: () =>
      import('@tusalud/app/features/notificaciones/notificaciones/notificaciones').then(
        (m) => m.Notificaciones,
      ),
  },
];
