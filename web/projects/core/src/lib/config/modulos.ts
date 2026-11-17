import { InjectionToken } from '@angular/core';

import { Rol } from '../models';

export interface Modulo {
  readonly app: string;
  readonly rol: Rol;
  readonly base: string;
  readonly etiqueta: string;
  readonly icono: string;
}

export const MODULOS: readonly Modulo[] = [
  {
    app: 'admin',
    rol: 'ROLE_ADMIN',
    base: '/admin/',
    etiqueta: 'Administración',
    icono: 'pi-cog',
  },
  {
    app: 'gerencia',
    rol: 'ROLE_GERENCIA',
    base: '/gerencia/',
    etiqueta: 'Gerencia',
    icono: 'pi-briefcase',
  },
  {
    app: 'medico',
    rol: 'ROLE_MEDICO',
    base: '/medico/',
    etiqueta: 'Consultorio',
    icono: 'pi-heart',
  },
  {
    app: 'enfermeria',
    rol: 'ROLE_ENFERMERIA',
    base: '/enfermeria/',
    etiqueta: 'Enfermería y hospitalización',
    icono: 'pi-building',
  },
  {
    app: 'finanzas',
    rol: 'ROLE_FINANZAS',
    base: '/finanzas/',
    etiqueta: 'Finanzas',
    icono: 'pi-money-bill',
  },
  {
    app: 'operaciones',
    rol: 'ROLE_OPERACIONES',
    base: '/operaciones/',
    etiqueta: 'Operaciones',
    icono: 'pi-sitemap',
  },
  {
    app: 'calidad',
    rol: 'ROLE_CALIDAD',
    base: '/calidad/',
    etiqueta: 'Calidad',
    icono: 'pi-verified',
  },
  {
    app: 'rrhh',
    rol: 'ROLE_RRHH',
    base: '/rrhh/',
    etiqueta: 'Recursos humanos',
    icono: 'pi-id-card',
  },
  {
    app: 'laboratorio',
    rol: 'ROLE_LABORATORIO',
    base: '/laboratorio/',
    etiqueta: 'Laboratorio',
    icono: 'pi-filter',
  },
  {
    app: 'imagenes',
    rol: 'ROLE_IMAGENES',
    base: '/imagenes/',
    etiqueta: 'Imágenes diagnósticas',
    icono: 'pi-image',
  },
  {
    app: 'farmacia',
    rol: 'ROLE_FARMACIA',
    base: '/farmacia/',
    etiqueta: 'Farmacia',
    icono: 'pi-plus-circle',
  },
  {
    app: 'almacen',
    rol: 'ROLE_ALMACEN',
    base: '/almacen/',
    etiqueta: 'Almacén',
    icono: 'pi-box',
  },
  {
    app: 'recepcion',
    rol: 'ROLE_RECEPCION',
    base: '/recepcion/',
    etiqueta: 'Recepción',
    icono: 'pi-users',
  },
  {
    app: 'caja',
    rol: 'ROLE_CAJA',
    base: '/caja/',
    etiqueta: 'Caja',
    icono: 'pi-wallet',
  },
  {
    app: 'paciente',
    rol: 'ROLE_PACIENTE',
    base: '/paciente/',
    etiqueta: 'Mi salud',
    icono: 'pi-user',
  },
];

export const PORTAL_LOGIN = '/auth/login';

export const APP_ACTUAL = new InjectionToken<string>('APP_ACTUAL');

export type NavegadorExterno = (url: string) => void;

export const NAVEGADOR_EXTERNO = new InjectionToken<NavegadorExterno>('NAVEGADOR_EXTERNO');

export function navegadorDelNavegador(): NavegadorExterno {
  return (url) => globalThis.location.assign(url);
}

export function modulosDe(roles: readonly Rol[]): readonly Modulo[] {
  const vistos = new Set<string>();
  return MODULOS.filter((modulo) => {
    if (!roles.includes(modulo.rol) || vistos.has(modulo.app)) {
      return false;
    }
    vistos.add(modulo.app);
    return true;
  });
}

export function moduloPrincipal(roles: readonly Rol[]): Modulo | null {
  return modulosDe(roles)[0] ?? null;
}

export function rolesDeApp(app: string): readonly Rol[] {
  return MODULOS.filter((modulo) => modulo.app === app).map((modulo) => modulo.rol);
}

export function destinoLogin(retorno: string | null, motivo?: string): string {
  const parametros: string[] = [];
  if (retorno && retorno.startsWith('/') && !retorno.startsWith('//')) {
    parametros.push(`returnUrl=${encodeURIComponent(retorno)}`);
  }
  if (motivo) {
    parametros.push(`motivo=${encodeURIComponent(motivo)}`);
  }
  return parametros.length > 0 ? `${PORTAL_LOGIN}?${parametros.join('&')}` : PORTAL_LOGIN;
}

export function retornoPermitido(
  retorno: string | null | undefined,
  roles: readonly Rol[],
): string | null {
  if (!retorno || !retorno.startsWith('/') || retorno.startsWith('//')) {
    return null;
  }
  const ruta = retorno.split(/[?#]/)[0];
  const permitido = modulosDe(roles).some(
    (modulo) => ruta === modulo.base.slice(0, -1) || ruta.startsWith(modulo.base),
  );
  return permitido ? retorno : null;
}
