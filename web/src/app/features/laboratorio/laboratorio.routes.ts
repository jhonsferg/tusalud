import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/app/core/guards/role.guard';

export const LABORATORIO_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Laboratorio',
    canMatch: [roleGuard('ROLE_LABORATORIO', 'ROLE_ADMIN', 'ROLE_MEDICO')],
    loadComponent: () =>
      import('@tusalud/app/features/laboratorio/ordenes/ordenes').then((m) => m.Ordenes),
  },
  {
    path: 'ordenes/:id',
    title: 'Orden de laboratorio',
    canMatch: [roleGuard('ROLE_LABORATORIO', 'ROLE_MEDICO')],
    loadComponent: () =>
      import('@tusalud/app/features/laboratorio/orden-detalle/orden-detalle').then(
        (m) => m.OrdenDetalle,
      ),
  },
  {
    path: 'examenes',
    title: 'Catálogo de exámenes',
    canMatch: [roleGuard('ROLE_LABORATORIO', 'ROLE_ADMIN', 'ROLE_MEDICO')],
    loadComponent: () =>
      import('@tusalud/app/features/laboratorio/examenes/examenes').then((m) => m.Examenes),
  },
  {
    path: 'solicitar',
    title: 'Solicitar exámenes',
    canMatch: [roleGuard('ROLE_MEDICO')],
    loadComponent: () =>
      import('@tusalud/app/features/laboratorio/solicitar/solicitar').then((m) => m.Solicitar),
  },
];

export const RESULTADOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis resultados',
    canMatch: [roleGuard('ROLE_PACIENTE')],
    loadComponent: () =>
      import('@tusalud/app/features/laboratorio/mis-resultados/mis-resultados').then(
        (m) => m.MisResultados,
      ),
  },
];
