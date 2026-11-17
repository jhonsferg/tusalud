import { ApplicationConfig } from '@angular/core';

import { provideTusalud } from '@tusalud/core';
import { environment } from '@tusalud/environment';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [provideTusalud({ app: 'portal', apiUrl: environment.apiUrl, rutas: routes })],
};
