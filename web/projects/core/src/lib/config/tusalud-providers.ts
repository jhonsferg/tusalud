import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeEsPe from '@angular/common/locales/es-PE';
import {
  EnvironmentProviders,
  LOCALE_ID,
  Provider,
  makeEnvironmentProviders,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  Route,
  Routes,
  TitleStrategy,
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
  withRouterConfig,
} from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { loadingInterceptor } from '../interceptors/loading.interceptor';
import { API_URL } from './api.config';
import { AppTitleStrategy } from './app-title.strategy';
import { APP_ACTUAL, NAVEGADOR_EXTERNO, navegadorDelNavegador } from './modulos';
import { serializadorRutaOculta } from './ruta-oculta';
import { PRIMENG_TRANSLATION_ES, TusaludPreset } from './theme';

export interface OpcionesTusalud {
  readonly app: string;
  readonly apiUrl: string;
  readonly rutas: Routes;
  readonly prefijoOculto?: string;
}

export function provideTusalud(opciones: OpcionesTusalud): EnvironmentProviders {
  registerLocaleData(localeEsPe);
  const proveedores: (Provider | EnvironmentProviders)[] = [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      opciones.rutas as Route[],
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
      withRouterConfig({ paramsInheritanceStrategy: 'always' }),
    ),
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    { provide: LOCALE_ID, useValue: 'es-PE' },
    { provide: API_URL, useValue: opciones.apiUrl },
    { provide: APP_ACTUAL, useValue: opciones.app },
    { provide: NAVEGADOR_EXTERNO, useFactory: navegadorDelNavegador },
    provideHttpClient(
      withFetch(),
      withInterceptors([loadingInterceptor, errorInterceptor, authInterceptor]),
    ),
    providePrimeNG({
      ripple: true,
      translation: PRIMENG_TRANSLATION_ES,
      theme: {
        preset: TusaludPreset,
        options: { darkModeSelector: '.app-dark', cssLayer: false },
      },
    }),
    MessageService,
    ConfirmationService,
  ];
  if (opciones.prefijoOculto) {
    proveedores.push(serializadorRutaOculta(opciones.prefijoOculto, opciones.rutas));
  }
  return makeEnvironmentProviders(proveedores);
}
