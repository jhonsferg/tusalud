import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'panel' },
  {
    path: 'panel',
    title: 'Panel de control',
    loadComponent: () => import('./panel/panel').then((m) => m.Panel),
  },
  {
    path: 'auditoria',
    title: 'Auditoría',
    loadComponent: () => import('./auditoria/auditoria').then((m) => m.Auditoria),
  },
  {
    path: 'seguridad',
    title: 'Seguridad',
    loadComponent: () => import('./seguridad/seguridad').then((m) => m.Seguridad),
  },
  {
    path: 'usuarios',
    title: 'Usuarios',
    loadComponent: () => import('./usuarios/usuarios').then((m) => m.Usuarios),
  },
  {
    path: 'cupones',
    title: 'Cupones de descuento',
    loadComponent: () => import('./cupones/cupones').then((m) => m.Cupones),
  },
  {
    path: 'pagos',
    title: 'Pagos',
    loadComponent: () => import('./pagos/pagos-admin').then((m) => m.PagosAdmin),
  },
];
