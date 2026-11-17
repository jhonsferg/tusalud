import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const HISTORIA_CLINICA_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi historia clínica',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('./historia-clinica/historia-clinica').then((m) => m.HistoriaClinica),
  },
];

export const PACIENTES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Mis pacientes',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () => import('./mis-pacientes/mis-pacientes').then((m) => m.MisPacientes),
  },
  {
    path: ':id/historia-clinica',
    title: 'Historia clínica del paciente',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () =>
      import('./historia-clinica/historia-clinica').then((m) => m.HistoriaClinica),
  },
];
