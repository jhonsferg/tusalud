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
import { AppTitleStrategy } from '@tusalud/app/core/config/app-title.strategy';
import { PRIMENG_TRANSLATION_ES, TusaludPreset } from '@tusalud/app/core/config/theme';
import { authInterceptor } from '@tusalud/app/core/interceptors/auth.interceptor';
import { errorInterceptor } from '@tusalud/app/core/interceptors/error.interceptor';
import { loadingInterceptor } from '@tusalud/app/core/interceptors/loading.interceptor';

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
    provideHttpClient(
      withFetch(),
      withInterceptors([loadingInterceptor, authInterceptor, errorInterceptor]),
    ),
    providePrimeNG({
      ripple: true,
      translation: PRIMENG_TRANSLATION_ES,
      theme: {
        preset: TusaludPreset,
        options: { darkModeSelector: false, cssLayer: false },
      },
    }),
    MessageService,
    ConfirmationService,
  ],
};
