import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const GESTION = roleGuard('ROLE_RECEPCION', 'ROLE_ADMIN');

export const RECEPCION_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Recepción',
    canMatch: [GESTION],
    loadComponent: () => import('./recepcion-panel/recepcion-panel').then((m) => m.RecepcionPanel),
  },
  {
    path: 'pacientes',
    title: 'Pacientes',
    canMatch: [GESTION],
    loadComponent: () =>
      import('./pacientes/pacientes-mostrador').then((m) => m.PacientesMostrador),
  },
  {
    path: 'citas/nueva',
    title: 'Reservar cita en mostrador',
    canMatch: [GESTION],
    loadComponent: () => import('./cita-mostrador/cita-mostrador').then((m) => m.CitaMostrador),
  },
  {
    path: 'pantalla',
    title: 'Sala de espera',
    canMatch: [GESTION],
    loadComponent: () => import('./pantalla/pantalla').then((m) => m.Pantalla),
  },
];

export const COLA_ROUTES: Routes = [
  {
    path: '',
    title: 'Cola de pacientes',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () => import('./cola-medico/cola-medico').then((m) => m.ColaMedico),
  },
];
