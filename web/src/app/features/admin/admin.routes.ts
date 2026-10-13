import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'auditoria' },
  {
    path: 'auditoria',
    title: 'Auditoría',
    loadComponent: () => import('./auditoria/auditoria').then((m) => m.Auditoria),
  },
];
