import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { Rol } from '../models';
import { AuthService, LOGIN_URL } from '../services/auth.service';

export const FORBIDDEN_URL = '/acceso-denegado';

export function roleGuard(...roles: readonly Rol[]): CanMatchFn {
  return (_route, segments) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      const returnUrl =
        router.currentNavigation()?.extractedUrl.toString() ??
        `/${segments.map((segment) => segment.path).join('/')}`;
      return router.createUrlTree([LOGIN_URL], { queryParams: { returnUrl } });
    }
    if (auth.hasAnyRole(roles)) {
      return true;
    }
    const inicio = auth.homeUrl();
    const destino = router.currentNavigation()?.extractedUrl.toString().split('?')[0];
    return router.parseUrl(destino === inicio ? FORBIDDEN_URL : inicio);
  };
}
