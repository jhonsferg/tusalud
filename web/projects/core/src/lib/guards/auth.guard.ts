import { Location } from '@angular/common';
import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router, UrlTree } from '@angular/router';

import { NAVEGADOR_EXTERNO, destinoLogin, modulosDe } from '../config/modulos';
import { AuthService, LOGIN_URL } from '../services/auth.service';

export function urlExterna(url: string): string {
  const router = inject(Router);
  const pedida = router.currentNavigation()?.initialUrl;
  const arbol = pedida ?? router.parseUrl(url);
  return inject(Location).prepareExternalUrl(router.serializeUrl(arbol));
}

export function irAlLogin(url: string): UrlTree | false {
  const navegador = inject(NAVEGADOR_EXTERNO, { optional: true });
  if (navegador) {
    navegador(destinoLogin(urlExterna(url)));
    return false;
  }
  return inject(Router).createUrlTree([LOGIN_URL], { queryParams: { returnUrl: url } });
}

export const authGuard: CanActivateFn & CanActivateChildFn = (_route, state) => {
  if (inject(AuthService).isAuthenticated()) {
    return true;
  }
  return irAlLogin(state.url);
};

export function entradaModulo(app: string): CanActivateFn & CanActivateChildFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    if (!auth.isAuthenticated()) {
      return irAlLogin(state.url);
    }
    const modulos = modulosDe(auth.roles());
    if (modulos.some((modulo) => modulo.app === app)) {
      return true;
    }
    const navegador = inject(NAVEGADOR_EXTERNO, { optional: true });
    if (navegador && modulos[0]) {
      navegador(modulos[0].base);
      return false;
    }
    return irAlLogin(state.url);
  };
}
