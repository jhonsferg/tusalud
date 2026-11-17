import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/app/core/guards/role.guard';

const GESTION = roleGuard('ROLE_ALMACEN', 'ROLE_ADMIN');

export const ALMACEN_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Almacén',
    canMatch: [GESTION],
    loadComponent: () =>
      import('@tusalud/app/features/almacen/almacen-panel/almacen-panel').then(
        (m) => m.AlmacenPanel,
      ),
  },
  {
    path: 'productos',
    title: 'Productos',
    canMatch: [roleGuard('ROLE_ALMACEN', 'ROLE_ADMIN', 'ROLE_FARMACIA')],
    loadComponent: () =>
      import('@tusalud/app/features/almacen/productos/productos').then((m) => m.Productos),
  },
  {
    path: 'proveedores',
    title: 'Proveedores',
    canMatch: [GESTION],
    loadComponent: () =>
      import('@tusalud/app/features/almacen/proveedores/proveedores').then((m) => m.Proveedores),
  },
  {
    path: 'ingresos',
    title: 'Ingresos de mercadería',
    canMatch: [GESTION],
    loadComponent: () =>
      import('@tusalud/app/features/almacen/ingresos/ingresos').then((m) => m.Ingresos),
  },
  {
    path: 'movimientos',
    title: 'Kardex',
    canMatch: [roleGuard('ROLE_ALMACEN', 'ROLE_ADMIN', 'ROLE_FARMACIA')],
    loadComponent: () =>
      import('@tusalud/app/features/almacen/movimientos/movimientos').then((m) => m.Movimientos),
  },
];
