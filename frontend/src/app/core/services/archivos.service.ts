import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ArchivoRecord } from '../models/mascota.model';
import { BffService } from './bff.service';

@Injectable({
  providedIn: 'root',
})
export class ArchivosService {
  constructor(private readonly bff: BffService) {}

  subirArchivo(archivo: File, mascotaId: number | string): Observable<ArchivoRecord> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.bff.postForm<ArchivoRecord>('/archivos/subir', formData, { mascotaId });
  }

  getArchivo(id: number | string): Observable<ArchivoRecord> {
    return this.bff.get<ArchivoRecord>(`/archivos/${id}`);
  }

  getArchivosMascota(mascotaId: number | string): Observable<ArchivoRecord[]> {
    return this.bff.get<ArchivoRecord[]>(`/archivos/mascota/${mascotaId}`);
  }

  descargarArchivo(id: number | string): Observable<Blob> {
    return this.bff.download(`/archivos/${id}/descargar`);
  }

  eliminarArchivo(id: number | string): Observable<unknown> {
    return this.bff.delete(`/archivos/${id}`);
  }
}