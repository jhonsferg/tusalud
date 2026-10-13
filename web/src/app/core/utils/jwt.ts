import { JwtPayload } from '@tusalud/app/core/models';

export function decodeJwt(token: string): JwtPayload | null {
  const parts = token.split('.');
  if (parts.length !== 3 || !parts[1]) {
    return null;
  }
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const bytes = Uint8Array.from(atob(padded), (char) => char.charCodeAt(0));
    const payload: unknown = JSON.parse(new TextDecoder().decode(bytes));
    return typeof payload === 'object' && payload !== null ? (payload as JwtPayload) : null;
  } catch {
    return null;
  }
}

export function jwtExpirationMs(token: string): number | null {
  const exp = decodeJwt(token)?.exp;
  return typeof exp === 'number' ? exp * 1000 : null;
}
