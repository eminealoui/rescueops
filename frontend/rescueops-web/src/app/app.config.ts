import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeFrTn from '@angular/common/locales/fr-TN';
import { ApplicationConfig, LOCALE_ID, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { apiInterceptor } from './core/http/api.interceptor';
import { errorInterceptor } from './core/http/error.interceptor';

registerLocaleData(localeFrTn);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    // Ordre : apiInterceptor ajoute le token, errorInterceptor traduit les erreurs.
    provideHttpClient(withInterceptors([apiInterceptor, errorInterceptor])),
    // Pas de resourceServer ici : c'est apiInterceptor (liste blanche /api) qui envoie le token.
    provideOAuthClient(),
    { provide: LOCALE_ID, useValue: 'fr-TN' },
    provideAppInitializer(() => inject(AuthService).init()),
  ],
};
