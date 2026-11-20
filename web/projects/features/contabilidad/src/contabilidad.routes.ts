import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const CONTABLE = roleGuard('ROLE_FINANZAS', 'ROLE_GERENCIA');

function ruta(titulo: string, cargar: () => Promise<unknown>): Routes {
  return [
    {
      path: '',
      title: titulo,
      canMatch: [CONTABLE],
      loadComponent: cargar as () => Promise<never>,
    },
  ];
}

export const CONTABILIDAD_RESUMEN_ROUTES: Routes = ruta('Contabilidad', () =>
  import('./resumen/resumen').then((m) => m.ContabilidadResumen),
);

export const ASIENTOS_ROUTES: Routes = ruta('Asientos contables', () =>
  import('./asientos/asientos').then((m) => m.Asientos),
);

export const PLAN_CUENTAS_ROUTES: Routes = ruta('Plan contable', () =>
  import('./plan-cuentas/plan-cuentas').then((m) => m.PlanCuentas),
);

export const PERIODOS_CONTABLES_ROUTES: Routes = ruta('Periodos contables', () =>
  import('./periodos/periodos').then((m) => m.PeriodosContables),
);

export const LIBROS_CONTABLES_ROUTES: Routes = ruta('Libros y estados financieros', () =>
  import('./libros/libros').then((m) => m.LibrosContables),
);

export const TESORERIA_ROUTES: Routes = ruta('Tesorería', () =>
  import('./tesoreria/tesoreria').then((m) => m.Tesoreria),
);

export const CONCILIACION_ROUTES: Routes = ruta('Conciliación bancaria', () =>
  import('./conciliacion/conciliacion').then((m) => m.Conciliacion),
);

export const FLUJO_CAJA_ROUTES: Routes = ruta('Flujo de caja', () =>
  import('./flujo-caja/flujo-caja').then((m) => m.FlujoCaja),
);

export const LIBROS_ELECTRONICOS_ROUTES: Routes = ruta('Libros electrónicos', () =>
  import('./libros-electronicos/libros-electronicos').then((m) => m.LibrosElectronicos),
);

export const PLANTILLAS_ASIENTO_ROUTES: Routes = ruta('Plantillas de asiento', () =>
  import('./plantillas-asiento/plantillas-asiento').then((m) => m.PlantillasAsiento),
);
