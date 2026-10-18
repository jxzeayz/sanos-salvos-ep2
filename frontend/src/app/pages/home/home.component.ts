import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../core/services/auth.service';

interface Tarjeta {
  icono: string;
  titulo: string;
  desc: string;
  to: string;
  label: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, MatCardModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent {
  readonly authService = inject(AuthService);

  tarjetas: Tarjeta[] = [
    {
      icono: 'pets',
      titulo: 'Reportar mascota',
      desc: 'Registra una mascota perdida o encontrada con sus características y ubicación.',
      to: '/mascotas/nueva',
      label: 'Registrar',
    },
    {
      icono: 'map',
      titulo: 'Ver mapa',
      desc: 'Visualiza los reportes en el mapa y detecta zonas con mayor incidencia.',
      to: '/mapa',
      label: 'Abrir mapa',
    },
    {
      icono: 'search',
      titulo: 'Coincidencias',
      desc: 'El sistema detecta automáticamente posibles coincidencias entre mascotas.',
      to: '/coincidencias',
      label: 'Ver coincidencias',
    },
  ];
}