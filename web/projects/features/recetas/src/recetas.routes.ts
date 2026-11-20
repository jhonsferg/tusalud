import { Routes } from '@angular/router';

export const RECETAS_MEDICO_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Recetas electrónicas',
    loadComponent: () => import('./recetas-medico/recetas-medico').then((m) => m.RecetasMedico),
  },
  {
    path: 'emitir/:citaId',
    title: 'Emitir receta electrónica',
    loadComponent: () => import('./emitir-receta/emitir-receta').then((m) => m.EmitirReceta),
  },
  {
    path: ':id',
    title: 'Receta electrónica',
    loadComponent: () => import('./receta-detalle/receta-detalle').then((m) => m.RecetaDetalle),
  },
];

export const MIS_RECETAS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Mis recetas',
    loadComponent: () => import('./mis-recetas/mis-recetas').then((m) => m.MisRecetas),
  },
  {
    path: ':id',
    title: 'Receta electrónica',
    loadComponent: () => import('./receta-detalle/receta-detalle').then((m) => m.RecetaDetalle),
  },
];

export const DISPENSACION_RECETAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Recetas electrónicas',
    loadComponent: () =>
      import('./dispensacion-receta/dispensacion-receta').then((m) => m.DispensacionReceta),
  },
];

export const CATALOGO_MEDICAMENTOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Catálogo de medicamentos',
    loadComponent: () =>
      import('./catalogo-medicamentos/catalogo-medicamentos').then((m) => m.CatalogoMedicamentos),
  },
];
