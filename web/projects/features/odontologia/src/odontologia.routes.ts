import { Routes } from '@angular/router';

export const ODONTOLOGIA_CLINICA_ROUTES: Routes = [
  {
    path: '',
    title: 'Odontograma',
    loadComponent: () =>
      import('./odontologia-clinica/odontologia-clinica').then((m) => m.OdontologiaClinica),
  },
];

export const PLANES_ODONTOLOGICOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Planes de tratamiento',
    loadComponent: () => import('./planes-clinica/planes-clinica').then((m) => m.PlanesClinica),
  },
];

export const MI_SALUD_DENTAL_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi salud dental',
    loadComponent: () =>
      import('./mis-planes-odontologicos/mis-planes-odontologicos').then(
        (m) => m.MisPlanesOdontologicos,
      ),
  },
];

export const COBROS_ODONTOLOGICOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Cobros odontológicos',
    loadComponent: () =>
      import('./cobros-odontologicos/cobros-odontologicos').then((m) => m.CobrosOdontologicos),
  },
];

export const PROCEDIMIENTOS_ODONTOLOGICOS_ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Procedimientos odontológicos',
    loadComponent: () =>
      import('./procedimientos-odontologicos/procedimientos-odontologicos').then(
        (m) => m.ProcedimientosOdontologicos,
      ),
  },
];
