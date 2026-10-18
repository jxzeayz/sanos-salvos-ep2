import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, switchMap, take } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Guard de rutas ADMIN. Exige sesión activa en Auth0 y que el rol se lea
 * del claim "https://sanosysalvos/rol" del access token vigente.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.auth0SdkReady().pipe(
    take(1),
    switchMap((autenticado) =>
      autenticado
        ? authService.obtenerToken().pipe(
            map((token) => ({ autenticado: true, token: token ?? null })),
            catchError(() => of({ autenticado: true, token: null as string | null }))
          )
        : of({ autenticado: false, token: null as string | null })
    ),
    map(({ autenticado, token }) => {
      if (!autenticado && !authService.isAuthenticated) {
        return router.createUrlTree(['/login']);
      }
      if (!authService.esRolAdmin(token)) {
        return router.createUrlTree(['/']);
      }
      return true;
    })
  );
};