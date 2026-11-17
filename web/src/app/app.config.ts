import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeEsPe from '@angular/common/locales/es-PE';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import {
  TitleStrategy,
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
  withRouterConfig,
} from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import {
  API_URL,
  AppTitleStrategy,
  authInterceptor,
  errorInterceptor,
  loadingInterceptor,
  PRIMENG_TRANSLATION_ES,
  TusaludPreset,
} from '@tusalud/core';
import { environment } from '../environments/environment';

registerLocaleData(localeEsPe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
      withRouterConfig({ paramsInheritanceStrategy: 'always' }),
    ),
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    { provide: LOCALE_ID, useValue: 'es-PE' },
    { provide: API_URL, useValue: environment.apiUrl },
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
  ],
};
