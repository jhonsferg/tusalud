import { Routes } from '@angular/router';

export const VACUNACION_CLINICA_ROUTES: Routes = [
  {
    path: '',
    title: 'Vacunación',
    loadComponent: () =>
      import('./vacunacion-clinica/vacunacion-clinica').then((m) => m.VacunacionClinica),
  },
];

export const CRECIMIENTO_CLINICA_ROUTES: Routes = [
  {
    path: '',
    title: 'Crecimiento',
    loadComponent: () =>
      import('./crecimiento-clinica/crecimiento-clinica').then((m) => m.CrecimientoClinica),
  },
];

export const MI_VACUNACION_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi carné de vacunación',
    loadComponent: () => import('./mi-vacunacion/mi-vacunacion').then((m) => m.MiVacunacion),
  },
];

export const MI_CRECIMIENTO_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi crecimiento',
    loadComponent: () => import('./mi-crecimiento/mi-crecimiento').then((m) => m.MiCrecimiento),
  },
];

export const ESQUEMAS_VACUNACION_ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Esquemas de vacunación',
    loadComponent: () =>
      import('./esquemas-vacunacion/esquemas-vacunacion').then((m) => m.EsquemasVacunacion),
  },
];
