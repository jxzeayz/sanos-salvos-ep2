import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { Coincidencia } from '../../core/models/coincidencia.model';
import { MascotasService } from '../../core/services/mascotas.service';
import { CoincidenciaListComponent } from '../../components/coincidencia-list/coincidencia-list.component';
import { extraerMensajeError } from '../../core/utils/error.util';

@Component({
  selector: 'app-coincidencias',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    FormsModule,
    CoincidenciaListComponent,
  ],
  templateUrl: './coincidencias.component.html',
  styleUrl: './coincidencias.component.css',
})
export class CoincidenciasComponent {
  mascotaId = '';
  coincidencias: Coincidencia[] = [];
  loading = false;
  error = '';
  buscado = false;

  constructor(private readonly mascotasService: MascotasService) {}

  buscar(): void {
    if (!this.mascotaId) return;
    this.loading = true;
    this.error = '';
    this.buscado = true;

    this.mascotasService.getCoincidencias(this.mascotaId).subscribe({
      next: (data) => {
        this.coincidencias = data || [];
      },
      error: (err) => {
        this.error = extraerMensajeError(err, 'Error al buscar coincidencias');
      },
      complete: () => {
        this.loading = false;
      },
    });
  }
}