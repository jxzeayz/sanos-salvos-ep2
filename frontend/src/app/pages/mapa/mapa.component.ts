import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MapaReportesComponent } from '../../components/mapa-reportes/mapa-reportes.component';

@Component({
  selector: 'app-mapa',
  standalone: true,
  imports: [CommonModule, MatCardModule, MapaReportesComponent],
  templateUrl: './mapa.component.html',
  styleUrl: './mapa.component.css',
})
export class MapaComponent {}