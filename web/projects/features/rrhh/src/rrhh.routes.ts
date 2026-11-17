import { Routes } from '@angular/router';

import { ROLES, Rol, roleGuard } from '@tusalud/core';

const PERSONAL: Rol[] = ROLES.filter((rol) => rol !== 'ROLE_PACIENTE');
const LECTURA = roleGuard('ROLE_RRHH', 'ROLE_GERENCIA', 'ROLE_ADMIN');
const GESTION = roleGuard('ROLE_RRHH', 'ROLE_ADMIN');

export const RRHH_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Recursos humanos',
    canMatch: [LECTURA],
    loadComponent: () => import('./resumen/resumen').then((m) => m.ResumenRrhh),
  },
  {
    path: 'empleados',
    title: 'Empleados',
    canMatch: [LECTURA],
    loadComponent: () => import('./empleados/empleados').then((m) => m.Empleados),
  },
  {
    path: 'turnos',
    title: 'Turnos del personal',
    canMatch: [GESTION],
    loadComponent: () => import('./turnos/turnos').then((m) => m.Turnos),
  },
  {
    path: 'ausencias',
    title: 'Ausencias',
    canMatch: [GESTION],
    loadComponent: () => import('./ausencias/ausencias').then((m) => m.Ausencias),
  },
];

export const MI_ESPACIO_ROUTES: Routes = [
  {
    path: '',
    canMatch: [roleGuard(...PERSONAL)],
    loadComponent: () => import('./mi-espacio/mi-espacio').then((m) => m.MiEspacio),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'turnos' },
      {
        path: 'turnos',
        title: 'Mis turnos',
        loadComponent: () => import('./mi-espacio/mis-turnos').then((m) => m.MisTurnos),
      },
      {
        path: 'ausencias',
        title: 'Mis ausencias',
        loadComponent: () => import('./mi-espacio/mis-ausencias').then((m) => m.MisAusencias),
      },
      {
        path: 'ficha',
        title: 'Mi ficha laboral',
        loadComponent: () => import('./mi-espacio/mi-ficha').then((m) => m.MiFicha),
      },
    ],
  },
];
