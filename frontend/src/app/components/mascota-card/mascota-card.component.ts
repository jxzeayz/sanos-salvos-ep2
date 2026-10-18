import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { Subscription } from 'rxjs';
import { Mascota } from '../../core/models/mascota.model';
import { ArchivosService } from '../../core/services/archivos.service';
import {
  ESPECIE_MASCOTA_LABEL,
  ESTADO_MASCOTA_LABEL,
} from '../../core/constants/app.constants';

const TAMANO_LABEL: Record<string, string> = {
  PEQUENO: 'Pequeño',
  MEDIANO: 'Mediano',
  GRANDE: 'Grande',
};

const ESTADO_COLOR_MATERIAL: Record<string, string> = {
  PERDIDA: 'warn',
  ENCONTRADA: 'primary',
  REUNIFICADA: 'accent',
};

const DEFAULT_IMG =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="160" viewBox="0 0 400 160"><rect fill="#e8eaf6" width="400" height="160"/><text fill="#7986cb" font-family="sans-serif" font-size="48" x="200" y="85" text-anchor="middle" dominant-baseline="central">🐾</text></svg>'
  );

@Component({
  selector: 'app-mascota-card',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './mascota-card.component.html',
  styleUrl: './mascota-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MascotaCardComponent implements OnInit, OnDestroy {
  @Input() mascota!: Mascota;
  @Input() usuarioId?: number;
  @Output() verCoincidencias = new EventEmitter<number>();
  @Output() eliminar = new EventEmitter<number>();
  @Output() cambiarEstado = new EventEmitter<{ id: number; estado: string }>();

  readonly DEFAULT_IMG = DEFAULT_IMG;
  readonly ESTADO_COLOR = ESTADO_COLOR_MATERIAL;

  fotoUrl: string | null = null;
  esDueno = false;
  fechaReporte = '—';
  puedoVerCoincidencias = false;

  private subs: Subscription[] = [];

  constructor(private readonly archivosService: ArchivosService) {}

  ngOnInit(): void {
    this.esDueno = this.usuarioId != null && String(this.mascota.usuarioId) === String(this.usuarioId);
    this.puedoVerCoincidencias =
      (this.mascota.estado === 'PERDIDA' || this.mascota.estado === 'ENCONTRADA') &&
      !!this.verCoincidencias.observed;

    if (this.mascota.fechaReporte) {
      this.fechaReporte = new Date(this.mascota.fechaReporte).toLocaleDateString('es-CL', {
        timeZone: 'America/Santiago',
      });
    }

    if (this.mascota.id) {
      this.subs.push(
        this.archivosService.getArchivosMascota(this.mascota.id).subscribe({
          next: (data) => {
            if (data && data.length > 0 && data[0].id) {
              this.subs.push(
                this.archivosService.descargarArchivo(data[0].id).subscribe({
                  next: (blob) => {
                    if (this.fotoUrl) {
                      URL.revokeObjectURL(this.fotoUrl);
                    }
                    this.fotoUrl = URL.createObjectURL(blob);
                  },
                  error: () => undefined,
                })
              );
            }
          },
          error: () => undefined,
        })
      );
    }
  }

  ngOnDestroy(): void {
    if (this.fotoUrl) {
      URL.revokeObjectURL(this.fotoUrl);
    }
    this.subs.forEach((s) => s.unsubscribe());
  }

  estadoLabel(estado?: string): string {
    return (estado && ESTADO_MASCOTA_LABEL[estado]) || estado || '';
  }

  estadoColor(estado?: string): string {
    return (estado && ESTADO_COLOR_MATERIAL[estado]) || '';
  }

  especieLabel(especie?: string): string {
    return (especie && ESPECIE_MASCOTA_LABEL[especie]) || especie || '';
  }

  tamanoLabel(tamano?: string): string {
    return (tamano && TAMANO_LABEL[tamano]) || tamano || '';
  }

  onVerCoincidencias(): void {
    if (this.mascota.id) {
      this.verCoincidencias.emit(this.mascota.id);
    }
  }

  onEliminar(): void {
    if (this.mascota.id) {
      this.eliminar.emit(this.mascota.id);
    }
  }

  onCambiarEstado(): void {
    if (this.mascota.id) {
      this.cambiarEstado.emit({ id: this.mascota.id, estado: 'REUNIFICADA' });
    }
  }
}