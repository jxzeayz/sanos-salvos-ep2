import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Coincidencia } from '../models/coincidencia.model';
import { Mascota, MascotaPayload } from '../models/mascota.model';
import { BffService } from './bff.service';

@Injectable({
  providedIn: 'root',
})
export class MascotasService {
  constructor(private readonly bff: BffService) {}

  getMascotas(estado?: string): Observable<Mascota[]> {
    return this.bff.get<Mascota[]>('/mascotas', { estado: estado ?? '' });
  }

  getMisMascotas(): Observable<Mascota[]> {
    return this.bff.get<Mascota[]>('/mascotas/mis-mascotas');
  }

  getMascota(id: number | string): Observable<Mascota> {
    return this.bff.get<Mascota>(`/mascotas/${id}`);
  }

  createMascota(data: MascotaPayload): Observable<Mascota> {
    return this.bff.post<Mascota>('/mascotas', data);
  }

  updateMascota(id: number | string, data: MascotaPayload): Observable<Mascota> {
    return this.bff.put<Mascota>(`/mascotas/${id}`, data);
  }

  deleteMascota(id: number | string): Observable<unknown> {
    return this.bff.delete(`/mascotas/${id}`);
  }

  updateEstado(id: number | string, estado: string): Observable<unknown> {
    return this.bff.patch(`/mascotas/${id}/estado?estado=${encodeURIComponent(estado)}`);
  }

  getCoincidencias(mascotaId: number | string): Observable<Coincidencia[]> {
    return this.bff.get<Coincidencia[]>(`/mascotas/${mascotaId}/coincidencias`);
  }

  getReportesMapa(): Observable<Mascota[]> {
    return this.bff.get<Mascota[]>('/mascotas/mapa');
  }
}