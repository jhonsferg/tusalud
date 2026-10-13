import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { SKIP_AUTH } from '@tusalud/app/core/config/http-context';
import { AuthService } from '@tusalud/app/core/services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.context.get(SKIP_AUTH) || !request.url.startsWith(inject(API_URL))) {
    return next(request);
  }
  const token = inject(AuthService).token();
  if (!token) {
    return next(request);
  }
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
