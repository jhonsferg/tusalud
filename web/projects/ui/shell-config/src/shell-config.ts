import {
  EnvironmentProviders,
  InjectionToken,
  Provider,
  makeEnvironmentProviders,
} from '@angular/core';

import { Rol } from '@tusalud/core';

export interface NavItem {
  readonly label: string;
  readonly icon: string;
  readonly link: string;
  readonly roles?: readonly Rol[];
  readonly exact?: boolean;
  readonly badge?: 'notificaciones';
}

export interface NavGroup {
  readonly title: string;
  readonly items: readonly NavItem[];
}

export interface ShellAccess {
  readonly label: string;
  readonly icon: string;
  readonly link: string;
  readonly tab: string | null;
}

export interface AppShellConfig {
  readonly brand: string;
  readonly grupos: readonly NavGroup[];
  readonly accesos?: readonly ShellAccess[];
}

export const APP_SHELL_CONFIG = new InjectionToken<AppShellConfig>('APP_SHELL_CONFIG');

export function provideAppShell(config: AppShellConfig): EnvironmentProviders {
  const proveedor: Provider = { provide: APP_SHELL_CONFIG, useValue: config };
  return makeEnvironmentProviders([proveedor]);
}
