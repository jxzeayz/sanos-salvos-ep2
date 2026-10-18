import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { publicGuard } from './core/guards/public.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'login',
    canActivate: [publicGuard],
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [publicGuard],
    loadComponent: () =>
      import('./pages/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'callback',
    loadComponent: () =>
      import('./pages/callback/callback.component').then((m) => m.CallbackComponent),
  },
  {
    path: 'mapa',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/mapa/mapa.component').then((m) => m.MapaComponent),
  },
  {
    path: 'mascotas',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/mascotas/mascotas.component').then((m) => m.MascotasComponent),
  },
  {
    path: 'mascotas/nueva',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/nueva-mascota/nueva-mascota.component').then((m) => m.NuevaMascotaComponent),
  },
  {
    path: 'mascotas/editar/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/nueva-mascota/nueva-mascota.component').then((m) => m.NuevaMascotaComponent),
  },
  {
    path: 'coincidencias',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/coincidencias/coincidencias.component').then((m) => m.CoincidenciasComponent),
  },
  {
    path: 'notificaciones',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/notificaciones/notificaciones.component').then((m) => m.NotificacionesComponent),
  },
  {
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/perfil/perfil.component').then((m) => m.PerfilComponent),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin/admin.component').then((m) => m.AdminComponent),
  },
  {
    path: 'admin/rabbitmq',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./pages/rabbit-admin/rabbit-admin.component').then((m) => m.RabbitAdminComponent),
  },
  { path: '**', redirectTo: '' },
];