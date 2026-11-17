import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const IMAGENES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Imágenes diagnósticas',
    canMatch: [roleGuard('ROLE_IMAGENES', 'ROLE_MEDICO')],
    loadComponent: () => import('./cola/cola').then((m) => m.ColaImagenes),
  },
  {
    path: 'agenda',
    title: 'Agenda de salas',
    canMatch: [roleGuard('ROLE_IMAGENES', 'ROLE_ADMIN', 'ROLE_RECEPCION')],
    loadComponent: () => import('./agenda/agenda-salas').then((m) => m.AgendaSalas),
  },
  {
    path: 'catalogo',
    title: 'Catálogo de estudios',
    canMatch: [roleGuard('ROLE_IMAGENES', 'ROLE_ADMIN', 'ROLE_MEDICO')],
    loadComponent: () => import('./catalogo/catalogo').then((m) => m.CatalogoImagenes),
  },
  {
    path: 'solicitar',
    title: 'Solicitar estudio de imagen',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () => import('./solicitar/solicitar').then((m) => m.SolicitarImagen),
  },
  {
    path: 'paciente/:pacienteId',
    title: 'Estudios del paciente',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () =>
      import('./estudios-paciente/estudios-paciente').then((m) => m.EstudiosPaciente),
  },
  {
    path: 'ordenes/:id',
    title: 'Orden de imagen',
    canMatch: [roleGuard('ROLE_IMAGENES', 'ROLE_MEDICO')],
    loadComponent: () => import('./orden-detalle/orden-detalle').then((m) => m.OrdenImagenDetalle),
  },
];

export const ESTUDIOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis estudios',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () => import('./mis-estudios/mis-estudios').then((m) => m.MisEstudios),
  },
];
