import { HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, of, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor HTTP: adjunta el access token vigente de Auth0 (IDaaS) a cada
 * petición hacia el BFF con la cabecera Authorization: Bearer <token>.
 *
 * El token se obtiene SIEMPRE a través del SDK de Auth0
 * (getAccessTokenSilently), que renueva automáticamente de forma silenciosa
 * (iframes/checkSession). De este modo nunca se reenvía un access token
 * expirado guardado en localStorage ni se depende de una copia manual.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);

  return authService.obtenerToken().pipe(
    switchMap((token) => {
      const conToken = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
      return next(conToken);
    }),
    catchError(() => next(req))
  );
};