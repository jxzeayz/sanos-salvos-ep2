import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Coincidencia } from '../../core/models/coincidencia.model';

@Component({
  selector: 'app-coincidencia-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './coincidencia-list.component.html',
  styleUrl: './coincidencia-list.component.css',
})
export class CoincidenciaListComponent {
  @Input() coincidencias: Coincidencia[] = [];
  @Input() loading = false;
  @Input() error = '';

  estadoIcono(estado: string): string {
    switch (estado) {
      case 'CONFIRMADA':
        return 'check_circle';
      case 'RECHAZADA':
        return 'cancel';
      default:
        return 'hourglass_empty';
    }
  }

  estadoColor(estado: string): string {
    switch (estado) {
      case 'CONFIRMADA':
        return 'primary';
      case 'RECHAZADA':
        return 'warn';
      default:
        return 'accent';
    }
  }

  scorePct(score?: number): number {
    return Math.round((score ?? 0) * 100);
  }

  scoreColor(score?: number): string {
    const pct = this.scorePct(score);
    if (pct >= 80) return 'primary';
    if (pct >= 60) return 'accent';
    return 'warn';
  }

  fechaDeteccion(fecha?: string): string {
    if (!fecha) return '';
    return new Date(fecha).toLocaleString('es-CL', { timeZone: 'America/Santiago' });
  }
}