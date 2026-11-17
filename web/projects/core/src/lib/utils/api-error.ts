import { HttpErrorResponse } from '@angular/common/http';

import { ApiError, ErrorResponse } from '../models';

const MENSAJES_POR_ESTADO: Readonly<Record<number, string>> = {
  0: 'No se pudo conectar con el servidor. Verifica tu conexión e inténtalo nuevamente.',
  400: 'La solicitud contiene datos inválidos.',
  401: 'Tu sesión no es válida. Inicia sesión nuevamente.',
  403: 'No tienes permisos para realizar esta operación.',
  404: 'El recurso solicitado no existe.',
  409: 'La operación entra en conflicto con el estado actual de los datos.',
  500: 'Ocurrió un error interno en el servidor.',
  502: 'El servidor no está disponible en este momento.',
  503: 'El servidor no está disponible en este momento.',
  504: 'El servidor tardó demasiado en responder.',
};

const MENSAJE_GENERICO = 'Ocurrió un error inesperado.';

function isErrorResponse(body: unknown): body is ErrorResponse {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as ErrorResponse).message === 'string' &&
    typeof (body as ErrorResponse).status === 'number'
  );
}

export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ApiError).status === 'number' &&
    typeof (value as ApiError).message === 'string' &&
    typeof (value as ApiError).validationErrors === 'object'
  );
}

export function defaultMessageForStatus(status: number): string {
  return MENSAJES_POR_ESTADO[status] ?? MENSAJE_GENERICO;
}

export function toApiError(error: HttpErrorResponse): ApiError {
  const body: unknown = error.error;
  if (isErrorResponse(body)) {
    return {
      status: error.status,
      message: body.message,
      path: body.path ?? null,
      timestamp: body.timestamp ?? null,
      validationErrors: body.validationErrors ?? {},
    };
  }
  return {
    status: error.status,
    message: defaultMessageForStatus(error.status),
    path: error.url,
    timestamp: null,
    validationErrors: {},
  };
}

export function asApiError(error: unknown): ApiError {
  if (isApiError(error)) {
    return error;
  }
  if (error instanceof HttpErrorResponse) {
    return toApiError(error);
  }
  return {
    status: -1,
    message: error instanceof Error && error.message ? error.message : MENSAJE_GENERICO,
    path: null,
    timestamp: null,
    validationErrors: {},
  };
}
