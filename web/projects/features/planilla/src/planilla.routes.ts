import { Routes } from '@angular/router';

import { roleGuard } from '@tusalud/core';

const GESTION = roleGuard('ROLE_RRHH', 'ROLE_FINANZAS', 'ROLE_GERENCIA');
const SOLO_RRHH = roleGuard('ROLE_RRHH');
const PERSONAL = roleGuard(
  'ROLE_MEDICO',
  'ROLE_ENFERMERIA',
  'ROLE_RECEPCION',
  'ROLE_CAJA',
  'ROLE_LABORATORIO',
  'ROLE_IMAGENES',
  'ROLE_FARMACIA',
  'ROLE_ALMACEN',
  'ROLE_FINANZAS',
  'ROLE_OPERACIONES',
  'ROLE_CALIDAD',
  'ROLE_RRHH',
  'ROLE_GERENCIA',
);

export const PLANILLAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Planilla mensual',
    canMatch: [GESTION],
    loadComponent: () => import('./planillas/planillas').then((m) => m.Planillas),
  },
  {
    path: ':id',
    title: 'Planilla',
    canMatch: [GESTION],
    loadComponent: () => import('./planillas/planilla-detalle').then((m) => m.PlanillaDetalle),
  },
];

export const CONTRATOS_LABORALES_ROUTES: Routes = [
  {
    path: '',
    title: 'Contratos laborales',
    canMatch: [SOLO_RRHH],
    loadComponent: () => import('./contratos/contratos').then((m) => m.ContratosLaborales),
  },
];

export const CONCEPTOS_PLANILLA_ROUTES: Routes = [
  {
    path: '',
    title: 'Conceptos de planilla',
    canMatch: [roleGuard('ROLE_RRHH', 'ROLE_FINANZAS')],
    loadComponent: () => import('./conceptos/conceptos').then((m) => m.ConceptosPlanilla),
  },
];

export const PARAMETROS_PLANILLA_ROUTES: Routes = [
  {
    path: '',
    title: 'Parámetros del régimen laboral',
    canMatch: [GESTION],
    loadComponent: () => import('./parametros/parametros').then((m) => m.ParametrosPlanilla),
  },
];

export const ASISTENCIA_PERSONAL_ROUTES: Routes = [
  {
    path: '',
    title: 'Asistencia del personal',
    canMatch: [SOLO_RRHH],
    loadComponent: () => import('./asistencia/asistencia').then((m) => m.AsistenciaPersonal),
  },
];

export const TERMINALES_ASISTENCIA_ROUTES: Routes = [
  {
    path: '',
    title: 'Terminales de asistencia',
    canMatch: [SOLO_RRHH],
    loadComponent: () => import('./terminales/terminales').then((m) => m.TerminalesAsistencia),
  },
];

export const MIS_BOLETAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis boletas de pago',
    canMatch: [PERSONAL],
    loadComponent: () => import('./mis-boletas/mis-boletas').then((m) => m.MisBoletas),
  },
];

export const MI_ASISTENCIA_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi asistencia',
    canMatch: [PERSONAL],
    loadComponent: () => import('./mi-asistencia/mi-asistencia').then((m) => m.MiAsistencia),
  },
];
