import { DOCUMENT, Injectable, effect, inject, signal } from '@angular/core';

import { StorageService } from './storage.service';

export const PRIVACIDAD_STORAGE_KEY = 'tusalud.privacidad';

@Injectable({ providedIn: 'root' })
export class PrivacidadService {
  private readonly storage = inject(StorageService);
  private readonly documento = inject(DOCUMENT);

  private readonly estado = signal(
    this.storage.read(
      PRIVACIDAD_STORAGE_KEY,
      (valor): valor is boolean => typeof valor === 'boolean',
    ) ?? false,
  );

  readonly activa = this.estado.asReadonly();

  constructor() {
    effect(() => {
      this.documento.documentElement.classList.toggle('sensible-activo', this.estado());
    });
  }

  alternar(): void {
    this.fijar(!this.estado());
  }

  fijar(activa: boolean): void {
    this.estado.set(activa);
    this.storage.write(PRIVACIDAD_STORAGE_KEY, activa);
  }
}

export function enmascararDni(valor: string | null | undefined): string {
  if (!valor) {
    return '';
  }
  const texto = valor.trim();
  if (texto.length <= 4) {
    return '*'.repeat(texto.length);
  }
  return `${texto.slice(0, 2)}${'*'.repeat(texto.length - 4)}${texto.slice(-2)}`;
}

export function enmascararTelefono(valor: string | null | undefined): string {
  if (!valor) {
    return '';
  }
  const digitos = valor.replace(/\D/g, '');
  return digitos.length <= 3 ? '*'.repeat(digitos.length) : `*** *** ${digitos.slice(-3)}`;
}

export function enmascararCorreo(valor: string | null | undefined): string {
  if (!valor) {
    return '';
  }
  const [usuario, dominio] = valor.split('@');
  if (!dominio) {
    return '*'.repeat(valor.length);
  }
  return `${usuario.slice(0, 1)}${'*'.repeat(Math.max(usuario.length - 1, 2))}@${dominio}`;
}
