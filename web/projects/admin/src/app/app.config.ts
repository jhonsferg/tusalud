import { ApplicationConfig } from '@angular/core';

import {
  PASOS_TOUR_PREDETERMINADOS,
  provideTourSteps,
  rolesDeApp,
  provideTusalud,
} from '@tusalud/core';
import { environment } from '@tusalud/environment';
import { provideAppShell } from '@tusalud/ui/shell-config';
import { APP } from './app.id';
import { NAV_GROUPS } from './nav.config';
import { routes } from './app.routes';

const ROLES = rolesDeApp(APP);

export const appConfig: ApplicationConfig = {
  providers: [
    provideTusalud({ app: APP, apiUrl: environment.apiUrl, rutas: routes, prefijoOculto: 'admin' }),
    provideAppShell({ brand: 'TuSalud', grupos: NAV_GROUPS }),
    provideTourSteps(
      PASOS_TOUR_PREDETERMINADOS.filter(
        (paso) => !paso.roles || paso.roles.some((rol) => ROLES.includes(rol)),
      ),
    ),
  ],
};
