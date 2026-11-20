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
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioGerencia),
      },
      {
        path: 'seguridad-paciente',
        canMatch: [roleGuard('ROLE_GERENCIA')],
        loadChildren: () =>
          import('@tusalud/features/seguridad-paciente').then((m) => m.EVENTOS_GESTION_ROUTES),
      },
      {
        path: 'reportes',
        canMatch: [roleGuard('ROLE_GERENCIA')],
        loadChildren: () => import('@tusalud/features/reportes').then((m) => m.REPORTES_ROUTES),
      },
      {
        path: 'rrhh',
        canMatch: [roleGuard('ROLE_GERENCIA')],
        loadChildren: () => import('@tusalud/features/rrhh').then((m) => m.RRHH_ROUTES),
      },
      {
        path: 'compras/ordenes',
        loadChildren: () =>
          import('@tusalud/features/compras').then((m) => m.ORDENES_COMPRA_ROUTES),
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
        path: 'planilla',
        loadChildren: () => import('@tusalud/features/planilla').then((m) => m.PLANILLAS_ROUTES),
      },
      {
        path: 'parametros-planilla',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.PARAMETROS_PLANILLA_ROUTES),
      },
      {
        path: 'mis-boletas',
        loadChildren: () => import('@tusalud/features/planilla').then((m) => m.MIS_BOLETAS_ROUTES),
      },
      {
        path: 'mi-asistencia',
        loadChildren: () =>
          import('@tusalud/features/planilla').then((m) => m.MI_ASISTENCIA_ROUTES),
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
        canMatch: [roleGuard('ROLE_GERENCIA')],
        loadChildren: () =>
          import('@tusalud/features/seguros').then((m) => m.SEGUROS_GERENCIA_ROUTES),
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
