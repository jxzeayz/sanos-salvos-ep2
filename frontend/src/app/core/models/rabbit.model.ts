/** Modelos de la API de administración de RabbitMQ (ms-rabbit-admin vía BFF). */

export interface ColaRabbit {
  nombre: string;
  mensajes: number;
  consumidores: number;
  durable?: boolean;
  argumentos?: Record<string, unknown>;
}

export interface ExchangeRabbit {
  nombre: string;
  tipo: string;
  durable: boolean;
  autoEliminar: boolean;
}

export interface BindingRabbit {
  exchange: string;
  cola: string;
  routingKey: string;
}

export interface MensajeRabbit {
  routingKey: string;
  exchange: string;
  reentregado: boolean;
  propiedades: Record<string, unknown>;
  payload: unknown;
}

export interface CrearColaRequest {
  nombre: string;
  durable?: boolean;
  ttlMensajesMs?: number | null;
  maxMensajes?: number | null;
  deadLetterExchange?: string | null;
  deadLetterRoutingKey?: string | null;
}

export interface CrearExchangeRequest {
  nombre: string;
  tipo: 'direct' | 'topic' | 'fanout' | 'headers';
  durable?: boolean;
}

export interface PublicarMensajeRequest {
  exchange: string;
  routingKey: string;
  payload: Record<string, unknown>;
}

/** Mismas reglas de nombres que valida el backend. */
export const PATRON_NOMBRE_RABBIT = /^(?!amq\.)[A-Za-z0-9._:-]{1,255}$/;
export const PATRON_ROUTING_KEY = /^[A-Za-z0-9._:*#-]{0,255}$/;
export const PATRON_ROUTING_KEY_PUBLICACION = /^[A-Za-z0-9._:-]{1,255}$/;
