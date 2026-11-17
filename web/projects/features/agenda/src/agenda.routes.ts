import { Routes } from '@angular/router';

export const AGENDA_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi agenda',
    loadComponent: () => import('./agenda/agenda').then((m) => m.Agenda),
  },
];
