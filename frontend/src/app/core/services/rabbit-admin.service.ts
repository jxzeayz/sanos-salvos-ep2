import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BindingRabbit,
  ColaRabbit,
  CrearColaRequest,
  CrearExchangeRequest,
  ExchangeRabbit,
  MensajeRabbit,
  PublicarMensajeRequest,
} from '../models/rabbit.model';
import { BffService } from './bff.service';

/** Cliente del administrador de RabbitMQ (rutas /bff/rabbit/**, solo ADMIN). */
@Injectable({
  providedIn: 'root',
})
export class RabbitAdminService {
  constructor(private readonly bff: BffService) {}

  listarColas(): Observable<ColaRabbit[]> {
    return this.bff.get<ColaRabbit[]>('/rabbit/queues');
  }

  crearCola(req: CrearColaRequest): Observable<ColaRabbit> {
    return this.bff.post<ColaRabbit>('/rabbit/queues', req);
  }

  eliminarCola(nombre: string): Observable<void> {
    return this.bff.delete<void>(`/rabbit/queues/${encodeURIComponent(nombre)}`);
  }

  purgarCola(nombre: string): Observable<{ cola: string; mensajesEliminados: number }> {
    return this.bff.delete(`/rabbit/queues/${encodeURIComponent(nombre)}/messages`);
  }

  leerMensajes(nombre: string, cantidad = 10): Observable<MensajeRabbit[]> {
    return this.bff.get<MensajeRabbit[]>(`/rabbit/queues/${encodeURIComponent(nombre)}/messages`, { cantidad });
  }

  listarExchanges(): Observable<ExchangeRabbit[]> {
    return this.bff.get<ExchangeRabbit[]>('/rabbit/exchanges');
  }

  crearExchange(req: CrearExchangeRequest): Observable<ExchangeRabbit> {
    return this.bff.post<ExchangeRabbit>('/rabbit/exchanges', req);
  }

  eliminarExchange(nombre: string): Observable<void> {
    return this.bff.delete<void>(`/rabbit/exchanges/${encodeURIComponent(nombre)}`);
  }

  listarBindings(): Observable<BindingRabbit[]> {
    return this.bff.get<BindingRabbit[]>('/rabbit/bindings');
  }

  crearBinding(req: BindingRabbit): Observable<BindingRabbit> {
    return this.bff.post<BindingRabbit>('/rabbit/bindings', req);
  }

  eliminarBinding(b: BindingRabbit): Observable<void> {
    return this.bff.delete<void>('/rabbit/bindings', {
      exchange: b.exchange,
      cola: b.cola,
      routingKey: b.routingKey,
    });
  }

  publicarMensaje(req: PublicarMensajeRequest): Observable<unknown> {
    return this.bff.post('/rabbit/messages', req);
  }
}
