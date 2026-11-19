import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const PERSONAL = roleGuard('ROLE_ADMIN', 'ROLE_CALIDAD');

export const MI_PRIVACIDAD_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi privacidad',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./mi-privacidad/mi-privacidad').then((m) => m.MiPrivacidad),
  },
];

export const SOLICITUDES_ARCO_ROUTES: Routes = [
  {
    path: '',
    title: 'Solicitudes de privacidad',
    canMatch: [PERSONAL],
    loadComponent: () =>
      import('./solicitudes-arco/solicitudes-arco').then((m) => m.SolicitudesArco),
  },
];

export const REGISTRO_TRATAMIENTOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Registro de tratamientos',
    canMatch: [PERSONAL],
    loadComponent: () =>
      import('./registro-tratamientos/registro-tratamientos').then(
        (m) => m.RegistroTratamientosPagina,
      ),
  },
];

export const RETENCION_DATOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Retención de datos',
    canMatch: [PERSONAL],
    loadComponent: () => import('./retencion-datos/retencion-datos').then((m) => m.RetencionDatos),
  },
];

export const INCIDENTES_DATOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Incidentes de datos personales',
    canMatch: [PERSONAL],
    loadComponent: () =>
      import('./incidentes-datos/incidentes-datos').then((m) => m.IncidentesDatos),
  },
];

export const POLITICA_ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Política de privacidad',
    canMatch: [PERSONAL],
    loadComponent: () => import('./politica-admin/politica-admin').then((m) => m.PoliticaAdmin),
  },
];

export const POLITICA_PUBLICA_ROUTES: Routes = [
  {
    path: '',
    title: 'Política de privacidad',
    loadComponent: () =>
      import('./politica-publica/politica-publica').then((m) => m.PoliticaPublica),
  },
];
