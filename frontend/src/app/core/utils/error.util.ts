/**
 * Extrae un mensaje de error legible desde un HttpErrorResponse,
 * replicando el comportamiento de `err.response?.data?.mensaje` de Axios.
 */
export function extraerMensajeError(err: unknown, fallback: string): string {
  if (err && typeof err === 'object') {
    const e = err as { error?: { mensaje?: string; message?: string }; message?: string };
    const mensaje = e.error?.mensaje || e.error?.message || e.message;
    if (mensaje) return mensaje;
  }
  return fallback;
}