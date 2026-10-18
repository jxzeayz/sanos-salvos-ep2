import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, combineLatest, delay, from, map, retry, switchMap, tap } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { AuthService as Auth0Service } from '@auth0/auth0-angular';
import {
  ChangePasswordRequest,
  UpdateProfileRequest,
  Usuario,
} from '../models/usuario.model';
import { BffService } from './bff.service';
import { environment } from '../../../environments/environment';

export interface AuthResponse {
  token?: string;
  usuarioId?: number;
  nombre?: string;
  email?: string;
  telefono?: string;
  rol?: string;
}

/**
 * Servicio de autenticación con IDaaS (Auth0).
 *
 * La autenticación y emisión del JWT la realiza Auth0 (Authorization Code
 * + PKCE). El BFF mantiene el perfil del usuario (nombre, teléfono, rol)
 * en la base de datos del proyecto, sincronizado por email desde el token.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private static readonly CLAIM_ROL = 'https://sanosysalvos/rol';
  private static readonly ROL_ADMIN = 'ADMIN';

  private readonly auth0 = inject(Auth0Service);
  private readonly bff = inject(BffService);

  private readonly tokenKey = 'token';
  private readonly userKey = 'usuario';

  private tokenSubject = new BehaviorSubject<string | null>(localStorage.getItem(this.tokenKey));
  private usuarioSubject = new BehaviorSubject<Usuario | null>(this.readUsuario());

  private sesionAuth0Activa = false;

  token$ = this.tokenSubject.asObservable();
  usuario$ = this.usuarioSubject.asObservable();

  isAuthenticated$ = this.auth0.isAuthenticated$;

  constructor() {
    // El SDK de Auth0 restaura la sesión al iniciar la aplicación;
    // mantenemos un snapshot para consultas síncronas.
    this.auth0.isAuthenticated$.subscribe((v) => (this.sesionAuth0Activa = v));
  }

  get token(): string | null {
    return this.tokenSubject.value;
  }

  get usuario(): Usuario | null {
    return this.usuarioSubject.value;
  }

  get isAuthenticated(): boolean {
    return Boolean(this.token);
  }

  get isSesionAuth0Activa(): boolean {
    return this.sesionAuth0Activa;
  }

  /**
   * Observable que emite TRUE/FALSE según el estado real de sesión de Auth0,
   * una vez que el SDK terminó de cargar (isLoading = false).
   */
  auth0SdkReady(): Observable<boolean> {
    return combineLatest([this.auth0.isLoading$, this.auth0.isAuthenticated$]).pipe(
      filter(([loading]) => !loading),
      take(1),
      map(([, autenticado]) => autenticado)
    );
  }

  /**
   * Indica si el usuario tiene rol ADMIN según el claim
   * "https://sanosysalvos/rol" del access token de Auth0 (no de datos
   * editables en localStorage).
   */
  get isAdmin(): boolean {
    return this.esRolAdmin(this.tokenSubject.value);
  }

  /**
   * Determina si el token (o el guardado en sesión, si no se entrega uno)
   * contiene el claim de rol ADMIN. El rol se lee exclusivamente de los
   * claims del token emitido por el IDaaS.
   */
  esRolAdmin(token: string | null | undefined): boolean {
    const valor = this.rolDesdeToken(token ?? this.tokenSubject.value);
    return valor === AuthService.ROL_ADMIN;
  }

  private rolDesdeToken(token: string | null): string | null {
    if (!token) return null;
    const payload = AuthService.decodificarPayload(token);
    if (!payload) return null;
    const rol = payload[AuthService.CLAIM_ROL] ?? payload['rol'];
    return rol != null ? String(rol) : null;
  }

  private static decodificarPayload(token: string): Record<string, unknown> | null {
    try {
      const parte = token.split('.')[1];
      if (!parte) return null;
      const normalizado = parte.replace(/-/g, '+').replace(/_/g, '/');
      const resto = normalizado.length % 4;
      const padded = resto === 2 ? normalizado + '==' : resto === 3 ? normalizado + '=' : normalizado;
      return JSON.parse(atob(padded));
    } catch {
      return null;
    }
  }

  /**
   * Redirige a la página de login de Auth0.
   */
  login(): void {
    this.auth0.loginWithRedirect({
      authorizationParams: { audience: environment.auth0.audience },
    });
  }

  /**
   * Redirige a la página de registro (signup) de Auth0.
   */
  register(): void {
    this.auth0.loginWithRedirect({
      authorizationParams: {
        audience: environment.auth0.audience,
        screen_hint: 'signup',
      },
    });
  }

  /**
   * Obtiene el access token de Auth0 de forma transparente
   * (renovación automática con refresh token).
   */
  obtenerToken(): Observable<string> {
    return from(
      this.auth0.getAccessTokenSilently({
        authorizationParams: { audience: environment.auth0.audience },
      })
    );
  }

  /**
   * Una vez autenticado en Auth0, sincroniza el perfil del usuario con el
   * backend (BFF) usando el email del token y guarda la sesión local.
   * Lanza el Observable con el perfil obtenido.
   */
  /**
   * Definido por el callback de Auth0: obtiene el access token, lo guarda en
   * sesión y sincroniza el perfil local con el BFF.
   */
  secuenciaInicioSesion(): Observable<AuthResponse> {
    return this.obtenerToken().pipe(
      // Justo tras el redirect el SDK puede fallar la primera lectura de la
      // caché de forma transitoria; se reintenta antes de abortar la sesión.
      retry({ count: 2, delay: 600 }),
      tap((token) => {
        localStorage.setItem(this.tokenKey, token);
        this.tokenSubject.next(token);
      }),
      switchMap(() => this.bff.post<AuthResponse>('/auth/sync', {})),
      map((perfil) => ({ ...perfil, token: this.tokenSubject.value ?? undefined })),
      tap((perfil) => this.saveSession(perfil))
    );
  }

  getProfile(): Observable<AuthResponse> {
    return this.bff.get<AuthResponse>('/auth/perfil');
  }

  updateProfile(datos: UpdateProfileRequest): Observable<unknown> {
    return this.bff.put('/auth/perfil', datos);
  }

  changePassword(datos: ChangePasswordRequest): Observable<unknown> {
    return this.bff.put('/auth/password', datos);
  }

  getUsuarios(): Observable<Usuario[]> {
    return this.bff.get<Usuario[]>('/auth/usuarios');
  }

  eliminarUsuario(id: number): Observable<unknown> {
    return this.bff.delete(`/auth/usuarios/${id}`);
  }

  updateUsuarioLocal(datos: Partial<Usuario>): void {
    const actual: Usuario | null = this.usuarioSubject.value;
    const actualizado: Usuario | null = actual
      ? {
          ...actual,
          ...datos,
          // El rol se gobierna por el claim del token de Auth0; no se
          // permite sobrescribirlo desde el perfil.
          rol: actual.rol,
        }
      : null;
    if (actualizado) {
      localStorage.setItem(this.userKey, JSON.stringify(actualizado));
      this.usuarioSubject.next(actualizado);
    }
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.tokenSubject.next(null);
    this.usuarioSubject.next(null);
    this.auth0.logout({ logoutParams: { returnTo: window.location.origin } });
  }

  private saveSession(res: AuthResponse): void {
    localStorage.setItem(this.tokenKey, res.token ?? '');
    localStorage.setItem(this.userKey, JSON.stringify(res));
    this.tokenSubject.next(res.token ?? null);
    this.usuarioSubject.next(res as Usuario);
  }

  private readUsuario(): Usuario | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as Usuario;
    } catch {
      return null;
    }
  }
}