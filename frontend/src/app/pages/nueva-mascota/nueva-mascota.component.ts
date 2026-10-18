import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MascotasService } from '../../core/services/mascotas.service';
import { Mascota } from '../../core/models/mascota.model';
import { MascotaFormComponent } from '../../components/mascota-form/mascota-form.component';

@Component({
  selector: 'app-nueva-mascota',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, MascotaFormComponent],
  templateUrl: './nueva-mascota.component.html',
  styleUrl: './nueva-mascota.component.css',
})
export class NuevaMascotaComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly mascotasService = inject(MascotasService);

  isEdit = false;
  mascota: Mascota | null = null;
  loading = false;
  error = '';
  id: string | null = null;

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id');
    this.isEdit = Boolean(this.id);

    if (this.id) {
      this.loading = true;
      this.mascotasService.getMascota(this.id).subscribe({
        next: (data) => {
          this.mascota = data;
        },
        error: () => {
          this.error = 'Error al cargar los datos de la mascota';
        },
        complete: () => {
          this.loading = false;
        },
      });
    }
  }
}