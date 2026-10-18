export const ESTADOS_MASCOTA = {
  PERDIDA: 'Perdida',
  ENCONTRADA: 'Encontrada',
  REUNIFICADA: 'Reunificada',
} as const;

export const ESTADO_MASCOTA_OPTIONS = [
  { value: 'PERDIDA', label: 'Perdida' },
  { value: 'ENCONTRADA', label: 'Encontrada' },
  { value: 'REUNIFICADA', label: 'Reunificada' },
];

export const ESPECIES_MASCOTA = ['Perro', 'Gato', 'Otro'] as const;

export const TAMANOS_MASCOTA = ['Pequeño', 'Mediano', 'Grande'] as const;

export const ROLES_USUARIO = {
  DUEÑO: 'DUEÑO',
  CIUDADANO: 'CIUDADANO',
  VETERINARIO: 'VETERINARIO',
  ADMIN: 'ADMIN',
} as const;

export const ROLES_OPTIONS = [
  { value: 'DUEÑO', label: 'Dueño' },
  { value: 'CIUDADANO', label: 'Ciudadano' },
  { value: 'VETERINARIO', label: 'Veterinario' },
  { value: 'ADMIN', label: 'Administrador' },
];

export const TIPO_NOTIFICACION = {
  COINCIDENCIA: 'COINCIDENCIA',
  MASCOTA_ENCONTRADA: 'MASCOTA_ENCONTRADA',
  MASCOTA_REUNIFICADA: 'MASCOTA_REUNIFICADA',
  COMENTARIO: 'COMENTARIO',
  SISTEMA: 'SISTEMA',
} as const;

export const TIPO_NOTIFICACION_LABEL: Record<string, string> = {
  COINCIDENCIA: 'Coincidencia detectada',
  MASCOTA_ENCONTRADA: 'Mascota encontrada',
  MASCOTA_REUNIFICADA: 'Mascota reunificada',
  COMENTARIO: 'Nuevo comentario',
  SISTEMA: 'Notificación del sistema',
};

export const COINCIDENCIA_ESTADO = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  RECHAZADA: 'Rechazada',
} as const;

export const ESTADO_MASCOTA_LABEL: Record<string, string> = {
  PERDIDA: 'Perdida',
  ENCONTRADA: 'Encontrada',
  REUNIFICADA: 'Reunificada',
  PEQUENO: 'Pequeño',
  MEDIANO: 'Mediano',
  GRANDE: 'Grande',
};

export const ESPECIE_MASCOTA_LABEL: Record<string, string> = {
  PERRO: 'Perro',
  GATO: 'Gato',
  OTRO: 'Otro',
};

export const ROL_USUARIO_LABEL: Record<string, string> = {
  DUEÑO: 'Dueño',
  CIUDADANO: 'Ciudadano',
  VETERINARIO: 'Veterinario',
  ADMIN: 'Administrador',
};

export const TIPO_NOTIFICACION_COLOR: Record<string, string> = {
  COINCIDENCIA: 'success',
  CONTACTO: 'info',
  ALERTA: 'warning',
  SISTEMA: 'info',
};

export const ESTADO_COLOR: Record<string, string> = {
  PERDIDA: 'error',
  ENCONTRADA: 'success',
  REUNIFICADA: 'info',
};