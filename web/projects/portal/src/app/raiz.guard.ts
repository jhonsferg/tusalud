import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService, NAVEGADOR_EXTERNO, PORTAL_LOGIN, moduloPrincipal } from '@tusalud/core';

export const raizPortal: CanActivateFn = () => {
  const auth = inject(AuthService);
  const modulo = auth.isAuthenticated() ? moduloPrincipal(auth.roles()) : null;
  if (modulo) {
    inject(NAVEGADOR_EXTERNO)(modulo.base);
    return false;
  }
  return inject(Router).parseUrl(PORTAL_LOGIN);
};
