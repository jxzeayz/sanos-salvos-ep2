export interface Usuario {
  token?: string;
  usuarioId?: number;
  nombre?: string;
  email?: string;
  telefono?: string;
  rol?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface UpdateProfileRequest {
  nombre: string;
  email: string;
  telefono?: string;
}