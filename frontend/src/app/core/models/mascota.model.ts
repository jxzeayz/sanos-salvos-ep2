export interface Mascota {
  id?: number;
  usuarioId?: number;
  nombre?: string;
  especie?: string;
  raza?: string;
  color?: string;
  descripcion?: string;
  tamano?: string;
  estado?: string;
  latitud?: number;
  longitud?: number;
  fechaReporte?: string;
}

export interface MascotaPayload {
  nombre: string;
  especie: string;
  raza?: string;
  color: string;
  descripcion?: string;
  tamano: string;
  estado: string;
  latitud?: number | null;
  longitud?: number | null;
}

export interface ArchivoRecord {
  id?: number;
  nombre?: string;
  tipo?: string;
  tamano?: number;
  mascotaId?: number;
}