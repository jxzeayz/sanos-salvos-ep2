import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

export type SeveridadSnackbar = 'success' | 'error' | 'warning' | 'info';

/**
 * Wrapper alrededor de MatSnackBar con colores por severidad,
 * equivalente al SnackbarContext de la versión React.
 */
@Injectable({
  providedIn: 'root',
})
export class SnackbarService {
  constructor(private readonly snackBar: MatSnackBar) {}

  show(message: string, severity: SeveridadSnackbar = 'success', duracionMs = 4000): void {
    const config: MatSnackBarConfig = {
      duration: duracionMs,
      panelClass: [`snackbar-${severity}`],
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
    };
    this.snackBar.open(message, '', config);
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error');
  }

  warning(message: string): void {
    this.show(message, 'warning');
  }

  info(message: string): void {
    this.show(message, 'info');
  }
}