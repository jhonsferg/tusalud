import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const GESTION = roleGuard('ROLE_ALMACEN', 'ROLE_ADMIN');

export const ALMACEN_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Almacén',
    canMatch: [GESTION],
    loadComponent: () => import('./almacen-panel/almacen-panel').then((m) => m.AlmacenPanel),
  },
  {
    path: 'productos',
    title: 'Productos',
    canMatch: [roleGuard('ROLE_ALMACEN', 'ROLE_ADMIN', 'ROLE_FARMACIA')],
    loadComponent: () => import('./productos/productos').then((m) => m.Productos),
  },
  {
    path: 'proveedores',
    title: 'Proveedores',
    canMatch: [GESTION],
    loadComponent: () => import('./proveedores/proveedores').then((m) => m.Proveedores),
  },
  {
    path: 'ingresos',
    title: 'Ingresos de mercadería',
    canMatch: [GESTION],
    loadComponent: () => import('./ingresos/ingresos').then((m) => m.Ingresos),
  },
  {
    path: 'movimientos',
    title: 'Kardex',
    canMatch: [roleGuard('ROLE_ALMACEN', 'ROLE_ADMIN', 'ROLE_FARMACIA')],
    loadComponent: () => import('./movimientos/movimientos').then((m) => m.Movimientos),
  },
];
