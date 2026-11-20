import { Routes } from '@angular/router';

export const NOTIFICAR_EVENTO_ROUTES: Routes = [
  {
    path: '',
    title: 'Notificar un evento de seguridad',
    loadComponent: () =>
      import('./notificar-evento/notificar-evento').then((m) => m.NotificarEvento),
  },
];

export const EVENTOS_GESTION_ROUTES: Routes = [
  {
    path: '',
    title: 'Seguridad del paciente',
    loadComponent: () => import('./eventos-gestion/eventos-gestion').then((m) => m.EventosGestion),
  },
];

export const GUIAS_CLINICAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Guías de práctica clínica',
    loadComponent: () => import('./guias-clinicas/guias-clinicas').then((m) => m.GuiasClinicas),
  },
];
