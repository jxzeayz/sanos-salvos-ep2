import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CrearNotificacionRequest, Notificacion } from '../models/notificacion.model';
import { BffService } from './bff.service';

@Injectable({
  providedIn: 'root',
})
export class NotificacionesService {
  constructor(private readonly bff: BffService) {}

  getNotificaciones(soloNoLeidas = false): Observable<Notificacion[]> {
    return this.bff.get<Notificacion[]>('/notificaciones', {
      soloNoLeidas: soloNoLeidas ? 'true' : '',
    });
  }

  getNotificacion(id: number): Observable<Notificacion> {
    return this.bff.get<Notificacion>(`/notificaciones/${id}`);
  }

  marcarLeida(id: number): Observable<unknown> {
    return this.bff.patch(`/notificaciones/${id}/leer`);
  }

  marcarTodasLeidas(): Observable<unknown> {
    return this.bff.patch('/notificaciones/leer-todas');
  }

  crearNotificacion(data: CrearNotificacionRequest): Observable<Notificacion> {
    return this.bff.post<Notificacion>('/notificaciones', data);
  }
}