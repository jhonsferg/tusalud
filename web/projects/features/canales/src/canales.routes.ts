import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

export const PLANTILLAS_MENSAJE_ROUTES: Routes = [
  {
    path: '',
    title: 'Plantillas de mensajes',
    canMatch: [roleGuard('ROLE_ADMIN')],
    loadComponent: () =>
      import('./plantillas-mensaje/plantillas-mensaje').then((m) => m.PlantillasMensajeAdmin),
  },
];

export const ENTREGAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Entregas de notificaciones',
    canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_OPERACIONES')],
    loadComponent: () =>
      import('./entregas/entregas-notificaciones').then((m) => m.EntregasNotificaciones),
  },
];

export const BAJA_CANAL_ROUTES: Routes = [
  {
    path: '',
    title: 'Dejar de recibir avisos',
    loadComponent: () => import('./baja-canal/baja-canal').then((m) => m.BajaCanal),
  },
];
