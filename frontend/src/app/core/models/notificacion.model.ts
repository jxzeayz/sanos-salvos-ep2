export interface Notificacion {
  id?: number;
  usuarioId?: number;
  origenUsuarioId?: number;
  tipo?: string;
  titulo?: string;
  mensaje?: string;
  leida?: boolean;
  enviadaEn?: string;
}

export interface CrearNotificacionRequest {
  usuarioId: number;
  origenUsuarioId: number;
  tipo: string;
  titulo: string;
  mensaje: string;
}