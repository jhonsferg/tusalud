import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { Rol } from '@tusalud/app/core/models';
import { AuthService, LOGIN_URL } from '@tusalud/app/core/services/auth.service';

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
    return auth.hasAnyRole(roles) ? true : router.parseUrl(FORBIDDEN_URL);
  };
}
