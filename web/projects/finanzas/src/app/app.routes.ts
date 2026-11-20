import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { AuthService, entradaModulo, roleGuard } from '@tusalud/core';
import { APP } from './app.id';

export const routes: Routes = [
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado',
    loadComponent: () => import('@tusalud/ui/forbidden').then((m) => m.Forbidden),
  },
  {
    path: '',
    canActivate: [entradaModulo(APP)],
    canActivateChild: [entradaModulo(APP)],
    loadComponent: () => import('@tusalud/ui/app-shell').then((m) => m.AppShell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => inject(AuthService).homeUrl(),
      },
      {
        path: 'inicio',
        title: 'Inicio',
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioFinanzas),
      },
      {
        path: 'reportes',
        canMatch: [roleGuard('ROLE_FINANZAS')],
        loadChildren: () => import('@tusalud/features/reportes').then((m) => m.REPORTES_ROUTES),
      },
      {
        path: 'compras/requisiciones',
        loadChildren: () => import('@tusalud/features/compras').then((m) => m.REQUISICIONES_ROUTES),
      },
      {
        path: 'compras/ordenes',
        loadChildren: () =>
          import('@tusalud/features/compras').then((m) => m.ORDENES_COMPRA_ROUTES),
      },
      {
        path: 'compras/recepciones',
        loadChildren: () => import('@tusalud/features/compras').then((m) => m.RECEPCIONES_ROUTES),
      },
      {
        path: 'compras/cuentas-por-pagar',
        loadChildren: () =>
          import('@tusalud/features/compras').then((m) => m.CUENTAS_POR_PAGAR_ROUTES),
      },
      {
        path: 'compras/reporte',
        loadChildren: () =>
          import('@tusalud/features/compras').then((m) => m.REPORTE_COMPRAS_ROUTES),
      },
      {
        path: 'contabilidad',
        pathMatch: 'full',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.CONTABILIDAD_RESUMEN_ROUTES),
      },
      {
        path: 'contabilidad/asientos',
        loadChildren: () => import('@tusalud/features/contabilidad').then((m) => m.ASIENTOS_ROUTES),
      },
      {
        path: 'contabilidad/plan-cuentas',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.PLAN_CUENTAS_ROUTES),
      },
      {
        path: 'contabilidad/periodos',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.PERIODOS_CONTABLES_ROUTES),
      },
      {
        path: 'contabilidad/libros',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.LIBROS_CONTABLES_ROUTES),
      },
      {
        path: 'contabilidad/tesoreria',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.TESORERIA_ROUTES),
      },
      {
        path: 'contabilidad/conciliacion',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.CONCILIACION_ROUTES),
      },
      {
        path: 'contabilidad/flujo-caja',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.FLUJO_CAJA_ROUTES),
      },
      {
        path: 'contabilidad/libros-electronicos',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.LIBROS_ELECTRONICOS_ROUTES),
      },
      {
        path: 'contabilidad/plantillas',
        loadChildren: () =>
          import('@tusalud/features/contabilidad').then((m) => m.PLANTILLAS_ASIENTO_ROUTES),
      },
      {
        path: 'mi-espacio',
        loadChildren: () => import('@tusalud/features/rrhh').then((m) => m.MI_ESPACIO_ROUTES),
      },
      {
        path: 'ausencias',
        redirectTo: 'mi-espacio/ausencias',
      },
      {
        path: 'seguros',
        canMatch: [roleGuard('ROLE_FINANZAS')],
        loadChildren: () =>
          import('@tusalud/features/seguros').then((m) => m.SEGUROS_FINANZAS_ROUTES),
      },
      {
        path: 'preautorizaciones',
        redirectTo: 'seguros/preautorizaciones',
      },
      {
        path: 'comprobantes-electronicos',
        loadChildren: () =>
          import('@tusalud/features/facturacion').then((m) => m.COMPROBANTES_ELECTRONICOS_ROUTES),
      },
      {
        path: 'resumenes-sunat',
        loadChildren: () =>
          import('@tusalud/features/facturacion').then((m) => m.RESUMENES_SUNAT_ROUTES),
      },
      {
        path: 'configuracion-fiscal',
        loadChildren: () =>
          import('@tusalud/features/facturacion').then((m) => m.CONFIGURACION_FISCAL_ROUTES),
      },
      {
        path: 'registro-ventas',
        loadChildren: () =>
          import('@tusalud/features/facturacion').then((m) => m.REGISTRO_VENTAS_ROUTES),
      },
      {
        path: 'calendario',
        loadChildren: () => import('@tusalud/features/calendario').then((m) => m.CALENDARIO_ROUTES),
      },
      {
        path: 'mis-exportaciones',
        loadChildren: () =>
          import('@tusalud/features/exportaciones').then((m) => m.EXPORTACIONES_ROUTES),
      },
      {
        path: 'notificaciones',
        loadChildren: () =>
          import('@tusalud/features/notificaciones').then((m) => m.NOTIFICACIONES_ROUTES),
      },
      {
        path: 'perfil',
        loadChildren: () => import('@tusalud/features/perfil').then((m) => m.PERFIL_ROUTES),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('@tusalud/ui/not-found').then((m) => m.NotFound),
  },
];
