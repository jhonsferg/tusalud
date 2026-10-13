import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';

import { AuthService, LOGIN_URL } from '@tusalud/app/core/services/auth.service';

export const authGuard: CanActivateFn & CanActivateChildFn = (_route, state) => {
  if (inject(AuthService).isAuthenticated()) {
    return true;
  }
  return inject(Router).createUrlTree([LOGIN_URL], { queryParams: { returnUrl: state.url } });
};
