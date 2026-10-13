import { HttpErrorResponse } from '@angular/common/http';

import { createJwt } from '../../../testing/auth-fixtures';
import { asApiError, isApiError, toApiError } from './api-error';
import {
  addDays,
  combineDateAndTime,
  parseIsoDate,
  parseIsoDateTime,
  toIsoDate,
  toIsoDateTime,
} from './date';
import { toHttpParams } from './http-params';
import { decodeJwt, jwtExpirationMs } from './jwt';
import { optionalNumberAttribute } from './route-input';

describe('jwt', () => {
  it('decodifica el payload con caracteres UTF-8', () => {
    const token = createJwt({ sub: 'maria@correo.com', nombre: 'María Ñandú', exp: 1_900_000_000 });
    expect(decodeJwt(token)).toEqual({
      sub: 'maria@correo.com',
      nombre: 'María Ñandú',
      exp: 1_900_000_000,
    });
    expect(jwtExpirationMs(token)).toBe(1_900_000_000_000);
  });

  it('devuelve null para tokens mal formados', () => {
    expect(decodeJwt('no-es-un-jwt')).toBeNull();
    expect(decodeJwt('a.%%%.c')).toBeNull();
    expect(jwtExpirationMs('a.b')).toBeNull();
  });
});

describe('date', () => {
  it('formatea fechas locales sin zona horaria', () => {
    const date = new Date(2026, 0, 5, 9, 7, 3);
    expect(toIsoDate(date)).toBe('2026-01-05');
    expect(toIsoDateTime(date)).toBe('2026-01-05T09:07:03');
  });

  it('parsea y valida fechas', () => {
    expect(parseIsoDate('2026-02-28')?.getDate()).toBe(28);
    expect(parseIsoDate('2026-02-30')).toBeNull();
    expect(parseIsoDate('28/02/2026')).toBeNull();
    expect(parseIsoDateTime('2026-10-20T10:30')?.getHours()).toBe(10);
    expect(parseIsoDateTime('2026-10-20')).toBeNull();
  });

  it('combina fecha y hora y suma días cruzando meses', () => {
    expect(combineDateAndTime('2026-10-20', '09:30')).toBe('2026-10-20T09:30:00');
    expect(addDays('2026-01-31', 1)).toBe('2026-02-01');
    expect(() => addDays('fecha', 1)).toThrow();
  });
});

describe('toHttpParams', () => {
  it('omite valores vacíos', () => {
    const params = toHttpParams({
      inicio: '2026-10-01',
      fin: '',
      especialidadId: 3,
      x: null,
      y: undefined,
    });
    expect(params.keys()).toEqual(['inicio', 'especialidadId']);
    expect(params.get('especialidadId')).toBe('3');
  });
});

describe('api-error', () => {
  it('usa el cuerpo estándar del backend', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        message: 'Error de validación',
        path: '/api/v1/citas',
        timestamp: '2026-10-12T20:00:00',
        validationErrors: { motivo: 'El motivo es obligatorio' },
      },
    });
    expect(toApiError(error)).toEqual({
      status: 400,
      message: 'Error de validación',
      path: '/api/v1/citas',
      timestamp: '2026-10-12T20:00:00',
      validationErrors: { motivo: 'El motivo es obligatorio' },
    });
  });

  it('usa un mensaje por defecto cuando el cuerpo no es del backend', () => {
    expect(toApiError(new HttpErrorResponse({ status: 0 })).message).toContain(
      'No se pudo conectar',
    );
    expect(toApiError(new HttpErrorResponse({ status: 502, error: '<html>' })).message).toContain(
      'no está disponible',
    );
    expect(toApiError(new HttpErrorResponse({ status: 418 })).message).toBe(
      'Ocurrió un error inesperado.',
    );
  });

  it('normaliza cualquier error', () => {
    const apiError = asApiError(new Error('fallo'));
    expect(isApiError(apiError)).toBe(true);
    expect(apiError.message).toBe('fallo');
    expect(asApiError(apiError)).toBe(apiError);
    expect(isApiError({ status: 1 })).toBe(false);
  });
});

describe('optionalNumberAttribute', () => {
  it('convierte identificadores válidos y descarta el resto', () => {
    expect(optionalNumberAttribute('7')).toBe(7);
    expect(optionalNumberAttribute(undefined)).toBeUndefined();
    expect(optionalNumberAttribute('')).toBeUndefined();
    expect(optionalNumberAttribute('abc')).toBeUndefined();
    expect(optionalNumberAttribute('-1')).toBeUndefined();
  });
});
