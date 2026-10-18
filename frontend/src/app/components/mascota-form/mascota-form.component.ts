import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Mascota, MascotaPayload } from '../../core/models/mascota.model';
import { MascotasService } from '../../core/services/mascotas.service';
import { ArchivosService } from '../../core/services/archivos.service';
import { SnackbarService } from '../../core/services/snackbar.service';
import { extraerMensajeError } from '../../core/utils/error.util';

@Component({
  selector: 'app-mascota-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './mascota-form.component.html',
  styleUrl: './mascota-form.component.css',
})
export class MascotaFormComponent implements OnInit {
  @Input() mascota: Mascota | null = null;
  @Input() isEdit = false;

  private readonly fb = inject(FormBuilder);
  private readonly mascotasService = inject(MascotasService);
  private readonly archivosService = inject(ArchivosService);
  private readonly router = inject(Router);
  private readonly snackbar = inject(SnackbarService);

  especies = [
    { value: 'PERRO', label: 'Perro' },
    { value: 'GATO', label: 'Gato' },
    { value: 'OTRO', label: 'Otro' },
  ];

  estados = [
    { value: 'PERDIDA', label: 'Perdida' },
    { value: 'ENCONTRADA', label: 'Encontrada' },
  ];

  tamanos = [
    { value: 'PEQUENO', label: 'Pequeño' },
    { value: 'MEDIANO', label: 'Mediano' },
    { value: 'GRANDE', label: 'Grande' },
  ];

  form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    especie: ['PERRO', Validators.required],
    raza: [''],
    color: ['', Validators.required],
    descripcion: [''],
    tamano: ['MEDIANO', Validators.required],
    estado: ['PERDIDA', Validators.required],
    latitud: [''],
    longitud: [''],
  });

  error = '';
  loading = false;
  foto: File | null = null;
  fotoPreview: string | null = null;
  private submitting = false;

  get controls() {
    return this.form.controls;
  }

  ngOnInit(): void {
    if (this.mascota) {
      this.form.patchValue({
        nombre: this.mascota.nombre || '',
        especie: this.mascota.especie || 'PERRO',
        raza: this.mascota.raza || '',
        color: this.mascota.color || '',
        descripcion: this.mascota.descripcion || '',
        tamano: this.mascota.tamano || 'MEDIANO',
        estado: this.mascota.estado || 'PERDIDA',
        latitud: this.mascota.latitud?.toString() || '',
        longitud: this.mascota.longitud?.toString() || '',
      });
    }
  }

  onGeolocate(): void {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.form.patchValue({
          latitud: pos.coords.latitude.toString(),
          longitud: pos.coords.longitude.toString(),
        });
      },
      () => {
        this.error = 'No se pudo obtener la ubicación';
      }
    );
  }

  onFotoChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.error = 'Solo se permiten archivos de imagen';
      this.clearFotoInput(input);
      return;
    }
    this.foto = file;
    this.fotoPreview = URL.createObjectURL(file);
  }

  onQuitarFoto(): void {
    this.foto = null;
    this.fotoPreview = null;
  }

  private clearFotoInput(input: HTMLInputElement): void {
    input.value = '';
  }

  onSubmit(): void {
    if (this.submitting || this.form.invalid) return;
    this.submitting = true;
    this.error = '';
    this.loading = true;

    const raw = this.form.getRawValue();
    const payload: MascotaPayload = {
      nombre: raw.nombre,
      especie: raw.especie,
      raza: raw.raza || '',
      color: raw.color,
      descripcion: raw.descripcion || '',
      tamano: raw.tamano,
      estado: raw.estado,
      latitud: raw.latitud ? parseFloat(raw.latitud) : null,
      longitud: raw.longitud ? parseFloat(raw.longitud) : null,
    };

    if (this.isEdit && this.mascota?.id) {
      this.mascotasService.updateMascota(this.mascota.id, payload).subscribe({
        next: () => {
          if (this.foto) {
            this.archivosService.subirArchivo(this.foto, this.mascota!.id!).subscribe({
              error: () => undefined,
            });
          }
          this.snackbar.success('Mascota actualizada correctamente');
          this.finalizar();
        },
        error: (err) => {
          this.error = extraerMensajeError(err, 'Error al actualizar la mascota');
          this.finalizar(false);
        },
      });
    } else {
      this.mascotasService.createMascota(payload).subscribe({
        next: (nueva) => {
          if (this.foto && nueva.id) {
            this.archivosService.subirArchivo(this.foto, nueva.id).subscribe({
              next: () => undefined,
              error: () => this.snackbar.warning('Mascota creada, pero hubo un error al subir la foto'),
            });
          }
          this.snackbar.success('Mascota registrada correctamente');
          this.finalizar();
        },
        error: (err) => {
          this.error = extraerMensajeError(err, 'Error al registrar la mascota');
          this.finalizar(false);
        },
      });
    }
  }

  private finalizar(exitoso = true): void {
    this.loading = false;
    this.submitting = false;
    if (exitoso) {
      this.router.navigate(['/mascotas']);
    }
  }
}