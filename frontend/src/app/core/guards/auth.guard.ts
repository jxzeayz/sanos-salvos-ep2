import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * Guard de autenticación. Espera a que el SDK de Auth0 resuelva la sesión
 * antes de decidir, para no redirigir prematuramente al login.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.auth0SdkReady().pipe(
    take(1),
    map((autenticado) => {
      if (autenticado || authService.isAuthenticated) {
        return true;
      }
      return router.createUrlTree(['/login']);
    })
  );
};