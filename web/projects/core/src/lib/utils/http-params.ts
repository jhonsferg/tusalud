import { HttpParams } from '@angular/common/http';

export type QueryValue = string | number | boolean | null | undefined;

export function toHttpParams(query: Readonly<Record<string, QueryValue>>): HttpParams {
  return Object.entries(query).reduce(
    (params, [key, value]) =>
      value === null || value === undefined || value === ''
        ? params
        : params.set(key, String(value)),
    new HttpParams(),
  );
}
