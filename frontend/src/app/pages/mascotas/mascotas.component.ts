import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { MascotasService } from '../../core/services/mascotas.service';
import { AuthService } from '../../core/services/auth.service';
import { SnackbarService } from '../../core/services/snackbar.service';
import { Mascota } from '../../core/models/mascota.model';
import { Coincidencia } from '../../core/models/coincidencia.model';
import { MascotaCardComponent } from '../../components/mascota-card/mascota-card.component';
import { CoincidenciaListComponent } from '../../components/coincidencia-list/coincidencia-list.component';

@Component({
  selector: 'app-mascotas',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatButtonToggleModule,
    MascotaCardComponent,
    CoincidenciaListComponent,
  ],
  templateUrl: './mascotas.component.html',
  styleUrl: './mascotas.component.css',
})
export class MascotasComponent implements OnInit {
  private readonly mascotasService = inject(MascotasService);
  private readonly authService = inject(AuthService);
  private readonly snackbar = inject(SnackbarService);

  filtros = [
    { value: 'todas', label: 'Todas' },
    { value: 'mis', label: 'Mis mascotas' },
    { value: 'PERDIDA', label: 'Perdidas' },
    { value: 'ENCONTRADA', label: 'Encontradas' },
    { value: 'REUNIFICADA', label: 'Reunificadas' },
  ];

  mascotas: Mascota[] = [];
  filtro = 'mis';
  busqueda = '';
  loading = true;
  error = '';
  usuarioId?: number;

  coincidencias: Coincidencia[] | null = null;
  loadingCoinc = false;
  errorCoinc = '';

  private busquedaSubject = new Subject<string>();

  ngOnInit(): void {
    this.usuarioId = this.authService.usuario?.usuarioId;

    this.busquedaSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
      this.cargarMascotas();
    });

    this.cargarMascotas();
  }

  onBusquedaChange(valor: string): void {
    this.busqueda = valor;
    this.busquedaSubject.next(valor);
  }

  onFiltroChange(valor: string): void {
    this.filtro = valor;
    this.cargarMascotas();
  }

  cargarMascotas(): void {
    this.loading = true;
    this.error = '';

    const obs =
      this.filtro === 'mis'
        ? this.mascotasService.getMisMascotas()
        : this.filtro === 'todas'
          ? this.mascotasService.getMascotas()
          : this.mascotasService.getMascotas(this.filtro);

    obs.subscribe({
      next: (data) => {
        let lista = data || [];
        if (this.busqueda) {
          const q = this.busqueda.toLowerCase();
          lista = lista.filter(
            (m) =>
              m.nombre?.toLowerCase().includes(q) ||
              m.especie?.toLowerCase().includes(q) ||
              m.raza?.toLowerCase().includes(q) ||
              m.color?.toLowerCase().includes(q)
          );
        }
        this.mascotas = lista;
      },
      error: () => {
        this.error = 'Error al cargar las mascotas';
      },
      complete: () => {
        this.loading = false;
      },
    });
  }

  verCoincidencias(mascotaId: number): void {
    this.coincidencias = null;
    this.errorCoinc = '';
    this.loadingCoinc = true;
    this.mascotasService.getCoincidencias(mascotaId).subscribe({
      next: (data) => {
        this.coincidencias = data || [];
      },
      error: () => {
        this.errorCoinc = 'Error al cargar coincidencias';
      },
      complete: () => {
        this.loadingCoinc = false;
      },
    });
  }

  eliminarMascota(id: number): void {
    if (!window.confirm('¿Estás seguro de eliminar esta mascota?')) return;
    this.mascotasService.deleteMascota(id).subscribe({
      next: () => {
        this.snackbar.success('Mascota eliminada correctamente');
        this.cargarMascotas();
      },
      error: () => {
        this.snackbar.error('Error al eliminar la mascota');
      },
    });
  }

  cambiarEstado(evento: { id: number; estado: string }): void {
    this.mascotasService.updateEstado(evento.id, evento.estado).subscribe({
      next: () => {
        this.snackbar.success(`Estado actualizado a ${evento.estado}`);
        this.cargarMascotas();
      },
      error: () => {
        this.snackbar.error('Error al actualizar estado');
      },
    });
  }

  filtroLabel(f: string): string {
    const enc = this.filtros.find((x) => x.value === f);
    return enc ? enc.label : f;
  }
}