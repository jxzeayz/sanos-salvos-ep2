import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription, interval } from 'rxjs';
import { Notificacion } from '../../core/models/notificacion.model';
import { NotificacionesService } from '../../core/services/notificaciones.service';
import { MascotasService } from '../../core/services/mascotas.service';
import { AuthService } from '../../core/services/auth.service';
import { SnackbarService } from '../../core/services/snackbar.service';
import { TIPO_NOTIFICACION_COLOR } from '../../core/constants/app.constants';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatListModule,
    MatDividerModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './notificaciones.component.html',
  styleUrl: './notificaciones.component.css',
})
export class NotificacionesComponent implements OnInit, OnDestroy {
  private readonly notificacionesService = inject(NotificacionesService);
  private readonly mascotasService = inject(MascotasService);
  private readonly authService = inject(AuthService);
  private readonly snackbar = inject(SnackbarService);

  notificaciones: Notificacion[] = [];
  loading = true;
  error = '';
  contactandoId: number | null = null;

  private readonly subs: Subscription[] = [];
  private readonly POLLING_MS = 30000;

  ngOnInit(): void {
    this.cargar();
    this.subs.push(interval(this.POLLING_MS).subscribe(() => this.cargar()));
  }

  ngOnDestroy(): void {
    this.subs.forEach((s) => s.unsubscribe());
  }

  get noLeidas(): number {
    return this.notificaciones.filter((n) => !n.leida).length;
  }

  cargar(): void {
    this.notificacionesService.getNotificaciones().subscribe({
      next: (data) => {
        this.notificaciones = data || [];
        this.error = '';
      },
      error: (err) => {
        const e = err as { error?: { mensaje?: string } };
        this.error = e.error?.mensaje || 'Error al cargar notificaciones';
      },
      complete: () => {
        this.loading = false;
      },
    });
  }

  marcarTodasLeidas(): void {
    this.notificacionesService.marcarTodasLeidas().subscribe({
      next: () => {
        this.notificaciones = this.notificaciones.map((n) => ({ ...n, leida: true }));
      },
      error: () => undefined,
    });
  }

  marcarLeida(id: number): void {
    this.notificacionesService.marcarLeida(id).subscribe({
      next: () => {
        this.notificaciones = this.notificaciones.map((n) =>
          n.id === id ? { ...n, leida: true } : n
        );
      },
      error: () => undefined,
    });
  }

  tipoColor(tipo: string): string {
    return TIPO_NOTIFICACION_COLOR[tipo] || 'accent';
  }

  fecha(enviadaEn?: string): string {
    if (!enviadaEn) return '';
    return new Date(enviadaEn).toLocaleString('es-CL', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'America/Santiago',
    });
  }

  puedeContactar(n: Notificacion): boolean {
    return n.tipo === 'COINCIDENCIA' && !!n.origenUsuarioId && !n.leida;
  }

  contactar(n: Notificacion): void {
    if (!n.origenUsuarioId || !this.authService.usuario?.usuarioId) return;
    this.contactandoId = n.id ?? null;

    this.mascotasService.getMisMascotas().subscribe({
      next: (mascotas) => {
        const perdidas = (mascotas || []).filter((m) => m.estado === 'PERDIDA');
        let nombreMascotas = 'mis mascotas';
        if (perdidas.length > 0) {
          nombreMascotas =
            perdidas.length === 1
              ? `mi mascota "${perdidas[0].nombre}"`
              : `mis mascotas "${perdidas.map((m) => m.nombre).join('", "')}"`;
        }
        this.enviarSolicitud(n, nombreMascotas);
      },
      error: () => {
        this.enviarSolicitud(n, 'mis mascotas');
      },
    });
  }

  private enviarSolicitud(n: Notificacion, nombreMascotas: string): void {
    const usuario = this.authService.usuario!;
    const nombreUsuario = usuario.nombre || 'un usuario';
    const contacto = usuario.telefono || usuario.email || '';
    const lineaContacto = contacto ? ` Mi datos de contacto: ${contacto}.` : '';

    this.notificacionesService
      .crearNotificacion({
        usuarioId: n.origenUsuarioId!,
        origenUsuarioId: usuario.usuarioId!,
        tipo: 'CONTACTO',
        titulo: 'Solicitud de contacto',
        mensaje: `Hola, soy ${nombreUsuario} y me gustaría ponerme en contacto contigo sobre ${nombreMascotas}.${lineaContacto}`,
      })
      .subscribe({
        next: () => {
          if (n.id) this.marcarLeida(n.id);
          this.snackbar.success('Solicitud de contacto enviada');
        },
        error: () => {
          this.snackbar.error('Error al enviar solicitud');
        },
        complete: () => {
          this.contactandoId = null;
        },
      });
  }
}