import { Routes } from '@angular/router';

export const ESTERILIZACION_ROUTES: Routes = [
  {
    path: '',
    title: 'Esterilización',
    loadComponent: () =>
      import('./esterilizacion-panel/esterilizacion-panel').then((m) => m.EsterilizacionPanel),
  },
];

export const CATALOGO_ESTERILIZACION_ROUTES: Routes = [
  {
    path: '',
    title: 'Esterilizadores y sets',
    loadComponent: () =>
      import('./catalogo-esterilizacion/catalogo-esterilizacion').then(
        (m) => m.CatalogoEsterilizacion,
      ),
  },
];
