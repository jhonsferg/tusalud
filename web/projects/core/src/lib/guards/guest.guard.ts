import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { NAVEGADOR_EXTERNO, moduloPrincipal } from '../config/modulos';
import { AuthService } from '../services/auth.service';

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.isAuthenticated()) {
    return true;
  }
  const navegador = inject(NAVEGADOR_EXTERNO, { optional: true });
  const modulo = moduloPrincipal(auth.roles());
  if (navegador && modulo) {
    navegador(modulo.base);
    return false;
  }
  return inject(Router).parseUrl(auth.homeUrl());
};
