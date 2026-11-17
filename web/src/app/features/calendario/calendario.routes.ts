import { Routes } from '@angular/router';

export const CALENDARIO_ROUTES: Routes = [
  {
    path: '',
    title: 'Calendario',
    loadComponent: () => import('./calendario-page/calendario-page').then((m) => m.CalendarioPage),
  },
];
