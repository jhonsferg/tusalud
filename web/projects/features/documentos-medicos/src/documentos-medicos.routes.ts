import { Routes } from '@angular/router';

export const DOCUMENTOS_MEDICO_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Documentos médicos',
    loadComponent: () =>
      import('./documentos-medico/documentos-medico').then((m) => m.DocumentosMedico),
  },
  {
    path: 'nuevo',
    title: 'Nuevo documento médico',
    loadComponent: () =>
      import('./documento-editor/documento-editor').then((m) => m.DocumentoEditor),
  },
  {
    path: ':id/editar',
    title: 'Editar borrador',
    loadComponent: () =>
      import('./documento-editor/documento-editor').then((m) => m.DocumentoEditor),
  },
  {
    path: ':id',
    title: 'Documento médico',
    loadComponent: () =>
      import('./documento-detalle/documento-detalle').then((m) => m.DocumentoDetalle),
  },
];

export const CONSENTIMIENTOS_MEDICO_ROUTES: Routes = [
  {
    path: '',
    title: 'Consentimientos informados',
    loadComponent: () =>
      import('./consentimientos-medico/consentimientos-medico').then(
        (m) => m.ConsentimientosMedico,
      ),
  },
];

export const DOCUMENTOS_PACIENTE_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Mis documentos',
    loadComponent: () => import('./mis-documentos/mis-documentos').then((m) => m.MisDocumentos),
  },
  {
    path: ':id',
    title: 'Documento médico',
    loadComponent: () =>
      import('./documento-detalle/documento-detalle').then((m) => m.DocumentoDetalle),
  },
];

export const CONSENTIMIENTOS_PACIENTE_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis consentimientos',
    loadComponent: () =>
      import('./mis-consentimientos/mis-consentimientos').then((m) => m.MisConsentimientos),
  },
];

export const ESTABLECIMIENTOS_ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Establecimientos de salud',
    loadComponent: () =>
      import('./establecimientos/establecimientos').then((m) => m.Establecimientos),
  },
];

export const PLANTILLAS_CONSENTIMIENTO_ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Plantillas de consentimiento',
    loadComponent: () =>
      import('./plantillas-consentimiento/plantillas-consentimiento').then(
        (m) => m.PlantillasConsentimiento,
      ),
  },
];

export const VERIFICACION_PUBLICA_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Verificar documento',
    loadComponent: () => import('./verificar/verificar').then((m) => m.Verificar),
  },
  {
    path: ':codigo',
    title: 'Verificar documento',
    loadComponent: () => import('./verificar/verificar').then((m) => m.Verificar),
  },
];
