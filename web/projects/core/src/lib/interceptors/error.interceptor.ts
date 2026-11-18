import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, from, map, switchMap, throwError } from 'rxjs';

import { SKIP_AUTH, SKIP_ERROR_NOTIFICATION } from '../config/http-context';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { toApiError } from '../utils/api-error';

function leerCuerpoBlob(error: HttpErrorResponse): Observable<HttpErrorResponse> {
  const blob = error.error as Blob;
  return from(blob.text()).pipe(
    map((texto) => {
      let cuerpo: unknown = null;
      try {
        cuerpo = JSON.parse(texto);
      } catch {
        cuerpo = null;
      }
      return new HttpErrorResponse({
        error: cuerpo,
        headers: error.headers,
        status: error.status,
        statusText: error.statusText,
        url: error.url ?? undefined,
      });
    }),
  );
}

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const notifications = inject(NotificationService);

  function procesar(error: HttpErrorResponse, solicitud: HttpRequest<unknown>): Observable<never> {
    const apiError = toApiError(error);
    const sessionRejected =
      apiError.status === 401 && !solicitud.context.get(SKIP_AUTH) && auth.isAuthenticated();

    if (sessionRejected) {
      auth.logout('unauthorized');
    } else if (!solicitud.context.get(SKIP_ERROR_NOTIFICATION)) {
      notifications.error(apiError.message);
    }
    return throwError(() => apiError);
  }

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }
      if (error.error instanceof Blob && error.error.type.includes('json')) {
        return leerCuerpoBlob(error).pipe(switchMap((legible) => procesar(legible, request)));
      }
      return procesar(error, request);
    }),
  );
};
