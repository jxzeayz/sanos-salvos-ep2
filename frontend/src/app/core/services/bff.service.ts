import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Cliente HTTP base hacia el BFF-Web.
 * Todas las rutas se construyen bajo la raíz `/bff`.
 */
@Injectable({
  providedIn: 'root',
})
export class BffService {
  private readonly base = '/bff';

  constructor(private readonly http: HttpClient) {}

  get<T>(path: string, params?: Record<string, string | number | boolean | null | undefined>): Observable<T> {
    return this.http.get<T>(this.resolve(path), { params: this.buildParams(params) });
  }

  post<T>(path: string, body?: unknown): Observable<T> {
    return this.http.post<T>(this.resolve(path), body);
  }

  put<T>(path: string, body?: unknown): Observable<T> {
    return this.http.put<T>(this.resolve(path), body);
  }

  patch<T>(path: string, body?: unknown): Observable<T> {
    return this.http.patch<T>(this.resolve(path), body);
  }

  /** DELETE con query params opcionales (se envían también los valores vacíos). */
  delete<T>(path: string, params?: Record<string, string | number | boolean | null | undefined>): Observable<T> {
    return this.http.delete<T>(this.resolve(path), { params: this.buildParams(params, true) });
  }

  postForm<T>(path: string, formData: FormData, params?: Record<string, string | number | boolean | null | undefined>): Observable<T> {
    return this.http.post<T>(this.resolve(path), formData, {
      params: this.buildParams(params),
    });
  }

  download(path: string, responseType: 'blob' = 'blob'): Observable<Blob> {
    return this.http.get(this.resolve(path), { responseType });
  }

  private resolve(path: string): string {
    return `${this.base}${path.startsWith('/') ? path : `/${path}`}`;
  }

  private buildParams(
    params?: Record<string, string | number | boolean | null | undefined>,
    incluirVacios = false,
  ): HttpParams {
    let hp = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== null && v !== undefined && (incluirVacios || v !== '')) {
          hp = hp.set(k, String(v));
        }
      });
    }
    return hp;
  }
}