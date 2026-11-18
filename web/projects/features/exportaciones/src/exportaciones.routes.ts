import { Routes } from '@angular/router';

export const EXPORTACIONES_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis exportaciones',
    loadComponent: () =>
      import('./mis-exportaciones/mis-exportaciones').then((m) => m.MisExportaciones),
  },
];
