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
        loadComponent: () => import('./inicio/inicio').then((m) => m.InicioAdmin),
      },
      {
        path: 'hce',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/hce').then((m) => m.HCE_ADMIN_ROUTES),
      },
      {
        path: 'guias',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/seguridad-paciente').then((m) => m.GUIAS_CLINICAS_ROUTES),
      },
      {
        path: 'admin',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/admin').then((m) => m.ADMIN_ROUTES),
      },
      {
        path: 'especialidades',
        canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
        loadChildren: () =>
          import('@tusalud/features/especialidades').then((m) => m.ESPECIALIDADES_ROUTES),
      },
      {
        path: 'medicos',
        canMatch: [roleGuard('ROLE_ADMIN', 'ROLE_PACIENTE')],
        loadChildren: () => import('@tusalud/features/medicos').then((m) => m.MEDICOS_ROUTES),
      },
      {
        path: 'citas',
        loadChildren: () => import('@tusalud/features/citas').then((m) => m.CITAS_ROUTES),
      },
      {
        path: 'recepcion',
        loadChildren: () => import('@tusalud/features/recepcion').then((m) => m.RECEPCION_ROUTES),
      },
      {
        path: 'caja',
        loadChildren: () => import('@tusalud/features/caja').then((m) => m.CAJA_ROUTES),
      },
      {
        path: 'imagenes',
        loadChildren: () => import('@tusalud/features/imagenes').then((m) => m.IMAGENES_ROUTES),
      },
      {
        path: 'hospitalizacion',
        loadChildren: () =>
          import('@tusalud/features/hospitalizacion').then((m) => m.HOSPITALIZACION_ROUTES),
      },
      {
        path: 'laboratorio',
        loadChildren: () =>
          import('@tusalud/features/laboratorio').then((m) => m.LABORATORIO_ROUTES),
      },
      {
        path: 'farmacia',
        loadChildren: () => import('@tusalud/features/farmacia').then((m) => m.FARMACIA_ROUTES),
      },
      {
        path: 'almacen',
        loadChildren: () => import('@tusalud/features/almacen').then((m) => m.ALMACEN_ROUTES),
      },
      {
        path: 'seguros',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/seguros').then((m) => m.SEGUROS_ADMIN_ROUTES),
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
        path: 'privacidad/solicitudes',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.SOLICITUDES_ARCO_ROUTES),
      },
      {
        path: 'privacidad/registro',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.REGISTRO_TRATAMIENTOS_ROUTES),
      },
      {
        path: 'privacidad/retencion',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.RETENCION_DATOS_ROUTES),
      },
      {
        path: 'privacidad/incidentes',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.INCIDENTES_DATOS_ROUTES),
      },
      {
        path: 'privacidad/politica',
        loadChildren: () =>
          import('@tusalud/features/privacidad').then((m) => m.POLITICA_ADMIN_ROUTES),
      },
      {
        path: 'calendario',
        loadChildren: () => import('@tusalud/features/calendario').then((m) => m.CALENDARIO_ROUTES),
      },
      {
        path: 'esquemas-vacunacion',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/pediatria').then((m) => m.ESQUEMAS_VACUNACION_ADMIN_ROUTES),
      },
      {
        path: 'catalogo-medicamentos',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/recetas').then((m) => m.CATALOGO_MEDICAMENTOS_ROUTES),
      },
      {
        path: 'procedimientos-odontologicos',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/odontologia').then(
            (m) => m.PROCEDIMIENTOS_ODONTOLOGICOS_ADMIN_ROUTES,
          ),
      },
      {
        path: 'cie10',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/cie10').then((m) => m.CIE10_ROUTES),
      },
      {
        path: 'reportes',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/reportes').then((m) => m.REPORTES_ROUTES),
      },
      {
        path: 'rrhh',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/rrhh').then((m) => m.RRHH_ROUTES),
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
        path: 'mi-espacio',
        loadChildren: () => import('@tusalud/features/rrhh').then((m) => m.MI_ESPACIO_ROUTES),
      },
      {
        path: 'ausencias',
        redirectTo: 'mi-espacio/ausencias',
      },
      {
        path: 'mis-exportaciones',
        loadChildren: () =>
          import('@tusalud/features/exportaciones').then((m) => m.EXPORTACIONES_ROUTES),
      },
      {
        path: 'establecimientos',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/documentos-medicos').then(
            (m) => m.ESTABLECIMIENTOS_ADMIN_ROUTES,
          ),
      },
      {
        path: 'consentimientos',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/documentos-medicos').then(
            (m) => m.PLANTILLAS_CONSENTIMIENTO_ADMIN_ROUTES,
          ),
      },
      {
        path: 'plantillas',
        loadChildren: () => import('@tusalud/features/plantillas').then((m) => m.PLANTILLAS_ROUTES),
      },
      {
        path: 'canales/plantillas',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/canales').then((m) => m.PLANTILLAS_MENSAJE_ROUTES),
      },
      {
        path: 'canales/entregas',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/canales').then((m) => m.ENTREGAS_ROUTES),
      },
      {
        path: 'respaldos',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () => import('@tusalud/features/respaldos').then((m) => m.RESPALDOS_ROUTES),
      },
      {
        path: 'notificaciones',
        loadChildren: () =>
          import('@tusalud/features/notificaciones').then((m) => m.NOTIFICACIONES_ROUTES),
      },
      {
        path: 'quirofanos',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/quirofano').then((m) => m.QUIROFANOS_ADMIN_ROUTES),
      },
      {
        path: 'esterilizacion-catalogo',
        canMatch: [roleGuard('ROLE_ADMIN')],
        loadChildren: () =>
          import('@tusalud/features/esterilizacion').then((m) => m.CATALOGO_ESTERILIZACION_ROUTES),
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
