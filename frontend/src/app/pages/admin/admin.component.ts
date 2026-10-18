import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { Mascota } from '../../core/models/mascota.model';
import { MascotasService } from '../../core/services/mascotas.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    RouterLink,
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  mascotas: Mascota[] = [];
  loading = true;
  error = '';

  displayedColumns = ['id', 'nombre', 'especie', 'estado', 'usuarioId', 'fecha'];

  get resumen(): { total: number; perdidas: number; encontradas: number; reunificadas: number } {
    return {
      total: this.mascotas.length,
      perdidas: this.mascotas.filter((m) => m.estado === 'PERDIDA').length,
      encontradas: this.mascotas.filter((m) => m.estado === 'ENCONTRADA').length,
      reunificadas: this.mascotas.filter((m) => m.estado === 'REUNIFICADA').length,
    };
  }

  constructor(private readonly mascotasService: MascotasService) {}

  ngOnInit(): void {
    this.mascotasService.getMascotas().subscribe({
      next: (data) => {
        this.mascotas = data || [];
      },
      error: () => {
        this.error = 'Error al cargar datos';
      },
      complete: () => {
        this.loading = false;
      },
    });
  }

  estadoColor(estado: string): string {
    if (estado === 'PERDIDA') return 'warn';
    if (estado === 'ENCONTRADA') return 'primary';
    return 'accent';
  }

  fecha(fechaReporte?: string): string {
    if (!fechaReporte) return '-';
    return new Date(fechaReporte).toLocaleDateString('es-CL', { timeZone: 'America/Santiago' });
  }
}