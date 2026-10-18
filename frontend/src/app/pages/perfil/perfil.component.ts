import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';
import { SnackbarService } from '../../core/services/snackbar.service';
import { extraerMensajeError } from '../../core/utils/error.util';

const ROL_LABEL: Record<string, string> = {
  DUEÑO: 'Dueño de mascota',
  CIUDADANO: 'Ciudadano colaborador',
  VETERINARIO: 'Veterinario / Clínica',
  ADMIN: 'Administrador',
};

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './perfil.component.html',
  styleUrl: './perfil.component.css',
})
export class PerfilComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly authService = inject(AuthService);
  private readonly snackbar = inject(SnackbarService);

  perfilForm = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telefono: ['', [Validators.pattern(/^\d*$/)]],
  });

  passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  });

  showCurrent = false;
  showNew = false;

  loadingProfile = false;
  loadingPassword = false;
  errorProfile = '';
  errorPassword = '';

  get rolLabel(): string {
    const rol = this.authService.usuario?.rol;
    return (rol && ROL_LABEL[rol]) || rol || '';
  }

  get inicial(): string {
    const nombre = this.authService.usuario?.nombre;
    return nombre ? nombre.charAt(0).toUpperCase() : 'U';
  }

  ngOnInit(): void {
    const u = this.authService.usuario;
    this.perfilForm.patchValue({
      nombre: u?.nombre || '',
      email: u?.email || '',
      telefono: u?.telefono || '',
    });
  }

  onTelefonoChange(): void {
    const val = this.perfilForm.controls.telefono.value ?? '';
    const soloDigitos = val.replace(/[^0-9]/g, '');
    if (val !== soloDigitos) {
      this.perfilForm.controls.telefono.setValue(soloDigitos);
    }
  }

  guardarPerfil(): void {
    if (this.perfilForm.invalid) return;
    this.errorProfile = '';
    this.loadingProfile = true;

    this.authService.updateProfile(this.perfilForm.getRawValue()).subscribe({
      next: () => {
        this.authService.updateUsuarioLocal(this.perfilForm.getRawValue());
        this.snackbar.success('Perfil actualizado correctamente');
      },
      error: (err) => {
        this.errorProfile = extraerMensajeError(err, 'Error al actualizar perfil');
      },
      complete: () => {
        this.loadingProfile = false;
      },
    });
  }

  cambiarPassword(): void {
    this.errorPassword = '';
    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.errorPassword = 'Las contraseñas no coinciden';
      return;
    }
    if (newPassword.length < 8) {
      this.errorPassword = 'La nueva contraseña debe tener al menos 8 caracteres';
      return;
    }
    this.loadingPassword = true;

    this.authService.changePassword(this.passwordForm.getRawValue()).subscribe({
      next: () => {
        this.snackbar.success('Contraseña cambiada exitosamente');
        this.passwordForm.reset();
      },
      error: (err) => {
        this.errorPassword = extraerMensajeError(err, 'Error al cambiar contraseña');
      },
      complete: () => {
        this.loadingPassword = false;
      },
    });
  }
}