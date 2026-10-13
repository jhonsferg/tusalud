import { AuthResponse, JwtPayload, Rol, Session } from '@tusalud/app/core/models';

function base64Url(value: string): string {
  const bytes = new TextEncoder().encode(value);
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join('');
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function createJwt(payload: JwtPayload): string {
  return [
    base64Url(JSON.stringify({ alg: 'HS512' })),
    base64Url(JSON.stringify(payload)),
    'firma',
  ].join('.');
}

export function createAuthResponse(
  roles: Rol[] = ['ROLE_PACIENTE'],
  expiresInMs = 60 * 60 * 1000,
  now = Date.now(),
): AuthResponse {
  return {
    token: createJwt({
      sub: 'juan.perez@correo.com',
      exp: Math.floor((now + expiresInMs) / 1000),
      uid: 4,
      nombre: 'Juan Pérez',
      roles,
    }),
    tokenType: 'Bearer',
    expiresIn: expiresInMs,
    usuarioId: 4,
    email: 'juan.perez@correo.com',
    nombreCompleto: 'Juan Pérez',
    roles,
  };
}

export function createSession(
  roles: Rol[] = ['ROLE_PACIENTE'],
  expiresInMs = 60 * 60 * 1000,
): Session {
  const response = createAuthResponse(roles, expiresInMs);
  return {
    token: response.token,
    expiresAt: Date.now() + expiresInMs,
    user: {
      id: response.usuarioId,
      email: response.email,
      nombreCompleto: response.nombreCompleto,
      roles,
    },
  };
}
