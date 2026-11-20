import { Routes } from '@angular/router';

export const QUIROFANO_ROUTES: Routes = [
  {
    path: '',
    title: 'Agenda quirúrgica',
    loadComponent: () =>
      import('./agenda-quirurgica/agenda-quirurgica').then((m) => m.AgendaQuirurgica),
  },
  {
    path: 'nueva',
    title: 'Solicitar cirugía',
    loadComponent: () =>
      import('./solicitud-cirugia/solicitud-cirugia').then((m) => m.SolicitudCirugia),
  },
  {
    path: ':id',
    title: 'Cirugía',
    loadComponent: () => import('./detalle-cirugia/detalle-cirugia').then((m) => m.DetalleCirugia),
  },
];

export const MIS_CIRUGIAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis cirugías',
    loadComponent: () => import('./mis-cirugias/mis-cirugias').then((m) => m.MisCirugias),
  },
];

export const QUIROFANOS_ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Quirófanos y equipos',
    loadComponent: () =>
      import('./quirofanos-admin/quirofanos-admin').then((m) => m.QuirofanosAdmin),
  },
];
