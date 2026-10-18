import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesService } from '../../core/services/notificaciones.service';
import { Notificacion } from '../../core/models/notificacion.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
})
export class NavbarComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);
  private readonly notificacionesService = inject(NotificacionesService);

  noLeidasCount = 0;
  private subscriptions: Subscription[] = [];

  ngOnInit(): void {
    this.authService.token$.subscribe((token) => {
      if (token) {
        this.cargarNoLeidas();
      } else {
        this.noLeidasCount = 0;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  private cargarNoLeidas(): void {
    this.subscriptions.push(
      this.notificacionesService.getNotificaciones().subscribe({
        next: (lista: Notificacion[]) => {
          this.noLeidasCount = lista.filter((n) => !n.leida).length;
        },
        error: () => {
          this.noLeidasCount = 0;
        },
      })
    );
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}