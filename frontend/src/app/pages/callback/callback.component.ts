import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService as Auth0Service } from '@auth0/auth0-angular';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';
import { SnackbarService } from '../../core/services/snackbar.service';
import { extraerMensajeError } from '../../core/utils/error.util';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="callback-contenedor">
      <mat-spinner diameter="48"></mat-spinner>
      <p>Completando inicio de sesión…</p>
    </div>
  `,
  styles: [
    `
      .callback-contenedor {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 16px;
        min-height: 60vh;
        color: #616161;
      }
    `,
  ],
})
export class CallbackComponent implements OnInit {
  private readonly auth0 = inject(Auth0Service);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackbar = inject(SnackbarService);

  ngOnInit(): void {
    this.auth0.handleRedirectCallback().subscribe({
      next: () => this.finalizarSesion(),
      // Sin parámetros de Auth0 (p. ej. sesión ya restaurada desde caché):
      // se sincroniza igualmente.
      error: () => this.finalizarSesion(),
    });
  }

  private finalizarSesion(): void {
    this.authService.secuenciaInicioSesion().subscribe({
      next: () => {
        this.snackbar.success('Inicio de sesión exitoso');
        this.router.navigate(['/mascotas']);
      },
      error: (err) => this.manejarError(err),
    });
  }

  private manejarError(err: unknown): void {
    this.snackbar.error(extraerMensajeError(err, 'Error al iniciar sesión con Auth0'));
    this.router.navigate(['/login']);
  }
}