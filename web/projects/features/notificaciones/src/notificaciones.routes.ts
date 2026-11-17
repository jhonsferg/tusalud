import { Routes } from '@angular/router';

export const NOTIFICACIONES_ROUTES: Routes = [
  {
    path: '',
    title: 'Notificaciones',
    loadComponent: () => import('./notificaciones/notificaciones').then((m) => m.Notificaciones),
  },
];
