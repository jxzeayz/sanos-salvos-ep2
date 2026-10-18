export interface Coincidencia {
  id?: number;
  mascotaPerdidaId?: number;
  mascotaEncontradaId?: number;
  scoreMatch?: number;
  estado?: string;
  fechaDeteccion?: string;
}

export interface AuditoriaLog {
  id?: number;
  usuarioId?: number;
  accion?: string;
  detalle?: string;
  usuario?: string;
  fecha?: string;
}