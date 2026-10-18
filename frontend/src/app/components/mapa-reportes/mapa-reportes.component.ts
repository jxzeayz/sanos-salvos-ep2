import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import * as L from 'leaflet';
import { Mascota } from '../../core/models/mascota.model';
import { MascotasService } from '../../core/services/mascotas.service';
import { ArchivosService } from '../../core/services/archivos.service';
import { ESPECIE_MASCOTA_LABEL } from '../../core/constants/app.constants';

interface Reporte extends Mascota {
  mascotaId?: number;
}

@Component({
  selector: 'app-mapa-reportes',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, MatChipsModule],
  templateUrl: './mapa-reportes.component.html',
  styleUrl: './mapa-reportes.component.css',
})
export class MapaReportesComponent implements OnInit, OnDestroy {
  private readonly mascotasService = inject(MascotasService);
  private readonly archivosService = inject(ArchivosService);

  reportes: Reporte[] = [];
  loading = true;
  error = '';
  private map?: L.Map;
  private fotoBlobUrls: string[] = [];

  private static readonly RADIO_ANONIMIZACION = 500;

  private static readonly iconoPerdida = L.icon({
    iconUrl:
      'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
    shadowUrl:
      'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
  });

  private static readonly iconoEncontrada = L.icon({
    iconUrl:
      'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png',
    shadowUrl:
      'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
  });

  ngOnInit(): void {
    this.cargarReportes();
  }

  ngOnDestroy(): void {
    this.fotoBlobUrls.forEach((u) => URL.revokeObjectURL(u));
    this.fotoBlobUrls = [];
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
  }

  private cargarReportes(): void {
    this.mascotasService.getReportesMapa().subscribe({
      next: (data) => {
        this.reportes = data || [];
      },
      error: () => {
        this.error = 'No se pudieron cargar los reportes del mapa';
      },
      complete: () => {
        this.loading = false;
        if (!this.error) {
          setTimeout(() => this.initMapa(), 0);
        }
      },
    });
  }

  private initMapa(): void {
    if (!this.map) {
      this.map = L.map('mapa-reportes', {
        center: this.calcularCentro(),
        zoom: 13,
      });
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution:
          '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(this.map);
    }

    this.reportes
      .filter((r) => r.latitud && r.longitud)
      .forEach((r) => this.agregarMarcador(r));

    this.map.invalidateSize();
  }

  private calcularCentro(): L.LatLngTuple {
    const primero = this.reportes.find((r) => r.latitud && r.longitud);
    if (primero) {
      return [primero.latitud!, primero.longitud!];
    }
    return [-33.45, -70.65];
  }

  private agregarMarcador(r: Reporte): void {
    const posicion: L.LatLngTuple = [r.latitud!, r.longitud!];
    const icono =
      r.estado === 'PERDIDA'
        ? MapaReportesComponent.iconoPerdida
        : MapaReportesComponent.iconoEncontrada;

    const marker = L.marker(posicion, { icon: icono }).addTo(this.map!);
    marker.bindPopup(this.contenidoPopup(r));
    marker.on('popupopen', (e) => {
      this.cargarFotoPopup(r, e.target as L.Marker);
    });

    L.circle(posicion, {
      radius: MapaReportesComponent.RADIO_ANONIMIZACION,
      color: r.estado === 'PERDIDA' ? 'red' : 'green',
      fillOpacity: 0.05,
    }).addTo(this.map!);
  }

  private contenidoPopup(r: Reporte): string {
    const especie = (r.especie && ESPECIE_MASCOTA_LABEL[r.especie]) || r.especie || '';
    const etiqueta = `${especie}${r.raza ? ` · ${r.raza}` : ''}`;
    const perdida = (r.estado || '').toLowerCase() === 'perdida';
    const estado = perdida ? 'Perdida' : 'Encontrada';
    return `
      <div style="min-width:200px;max-width:260px">
        <strong style="font-size:14px">${r.nombre || 'Mascota'}</strong><br/>
        <span style="font-size:12px;color:#666">${etiqueta}</span><br/>
        <span style="display:inline-block;font-size:11px;padding:2px 6px;border-radius:4px;background:${
          perdida ? '#ffebee' : '#e8f5e9'
        };color:${perdida ? '#c62828' : '#2e7d32'};margin:4px 0">${estado}</span><br/>
        ${r.color ? `<span style="font-size:12px;color:#666">Color: ${r.color}<br/></span>` : ''}
        ${r.descripcion ? `<span style="font-size:12px;color:#444;font-style:italic">"${r.descripcion}"</span>` : ''}
        <div id="foto-${r.id}"></div>
      </div>
    `;
  }

  private cargarFotoPopup(r: Reporte, marker: L.Marker): void {
    const contenedor = document.getElementById(`foto-${r.id}`);
    if (!contenedor || contenedor.childElementCount > 0) return;

    const mascotaId = r.mascotaId ?? r.id;
    if (!mascotaId) return;

    this.archivosService.getArchivosMascota(mascotaId).subscribe({
      next: (archivos) => {
        if (!archivos || archivos.length === 0) return;
        this.archivosService.descargarArchivo(archivos[0].id!).subscribe({
          next: (blob) => {
            const url = URL.createObjectURL(blob);
            this.fotoBlobUrls.push(url);
            contenedor.innerHTML = `<img src="${url}" alt="${r.nombre || ''}" style="width:100%;height:120px;object-fit:contain;border-radius:4px;margin-bottom:8px;background:#f5f5f5"/>`;
            const revocar = () => {
              const idx = this.fotoBlobUrls.indexOf(url);
              if (idx >= 0) {
                this.fotoBlobUrls.splice(idx, 1);
                URL.revokeObjectURL(url);
              }
            };
            marker.once('popupclose', revocar);
            marker.openPopup();
          },
          error: () => undefined,
        });
      },
      error: () => undefined,
    });
  }
}