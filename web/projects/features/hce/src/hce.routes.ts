import { Routes } from '@angular/router';

export const HISTORIAL_CLINICO_ROUTES: Routes = [
  {
    path: ':atencionId',
    title: 'Historial de la atención',
    data: { edicion: true },
    loadComponent: () =>
      import('./historial-atencion/historial-atencion').then((m) => m.HistorialAtencionPagina),
  },
];

export const HCE_ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Historia clínica electrónica',
    loadComponent: () => import('./hce-admin/hce-admin').then((m) => m.HceAdmin),
  },
];

export const MI_HISTORIA_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi historia clínica',
    loadComponent: () => import('./mi-historia/mi-historia').then((m) => m.MiHistoria),
  },
];
