import { HttpContext, HttpContextToken } from '@angular/common/http';

export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);

export const SKIP_ERROR_NOTIFICATION = new HttpContextToken<boolean>(() => false);

export const SKIP_LOADING = new HttpContextToken<boolean>(() => false);

export function publicRequest(): HttpContext {
  return new HttpContext().set(SKIP_AUTH, true).set(SKIP_ERROR_NOTIFICATION, true);
}
