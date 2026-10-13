import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { SKIP_AUTH, SKIP_ERROR_NOTIFICATION } from '../config/http-context';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { toApiError } from '../utils/api-error';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const notifications = inject(NotificationService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }
      const apiError = toApiError(error);
      const sessionRejected =
        apiError.status === 401 && !request.context.get(SKIP_AUTH) && auth.isAuthenticated();

      if (sessionRejected) {
        auth.logout('unauthorized');
      } else if (!request.context.get(SKIP_ERROR_NOTIFICATION)) {
        notifications.error(apiError.message);
      }
      return throwError(() => apiError);
    }),
  );
};
