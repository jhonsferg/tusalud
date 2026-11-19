import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const PARTICIPANTES = roleGuard('ROLE_PACIENTE', 'ROLE_MEDICO');

export const TELECONSULTAS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Teleconsultas',
    canMatch: [PARTICIPANTES],
    loadComponent: () => import('./lista/lista-teleconsultas').then((m) => m.ListaTeleconsultas),
  },
  {
    path: ':id/prueba',
    title: 'Preparar la teleconsulta',
    canMatch: [PARTICIPANTES],
    loadComponent: () => import('./prueba/prueba-dispositivos').then((m) => m.PruebaDispositivos),
  },
  {
    path: ':id/sala',
    title: 'Teleconsulta',
    canMatch: [PARTICIPANTES],
    loadComponent: () => import('./sala/sala-teleconsulta').then((m) => m.SalaTeleconsulta),
  },
];
