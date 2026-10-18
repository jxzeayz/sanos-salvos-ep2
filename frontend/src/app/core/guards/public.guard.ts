import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * Redirige a usuarios ya autenticados fuera de login/registro.
 */
export const publicGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.auth0SdkReady().pipe(
    take(1),
    map((autenticado) => {
      if (autenticado || authService.isAuthenticated) {
        return router.createUrlTree(['/mascotas']);
      }
      return true;
    })
  );
};