import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { SKIP_AUTH } from '@tusalud/app/core/config/http-context';
import { AuthService } from '@tusalud/app/core/services/auth.service';

function conToken(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.context.get(SKIP_AUTH) || !request.url.startsWith(inject(API_URL))) {
    return next(request);
  }
  const auth = inject(AuthService);
  const token = auth.token();
  if (!token) {
    return next(request);
  }
  return next(conToken(request, token)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      const actual = auth.token();
      if (actual && actual !== token) {
        return next(conToken(request, actual));
      }
      if (!auth.puedeRenovar()) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap((nuevo) => next(conToken(request, nuevo))),
        catchError(() => throwError(() => error)),
      );
    }),
  );
};
