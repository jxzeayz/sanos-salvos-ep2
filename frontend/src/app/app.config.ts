import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideAuth0 } from '@auth0/auth0-angular';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimations(),
    provideAuth0({
      domain: environment.auth0.domain,
      clientId: environment.auth0.clientId,
      authorizationParams: {
        redirect_uri: environment.auth0.redirectUri,
        audience: environment.auth0.audience,
      },
      cacheLocation: 'localstorage',
      // Renovación silenciosa vía iframe (checkSession). No se depende de
      // refresh tokens, que exigen "Refresh Token Rotation" activado en el
      // tenant de Auth0; sin él, getAccessTokenSilently lanza
      // "Missing Refresh Token" y bloquea el inicio de sesión.
      useRefreshTokens: false,
    }),
  ],
};