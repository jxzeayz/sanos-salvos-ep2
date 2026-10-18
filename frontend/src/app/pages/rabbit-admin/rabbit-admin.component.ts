import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  BindingRabbit,
  ColaRabbit,
  CrearColaRequest,
  CrearExchangeRequest,
  ExchangeRabbit,
  MensajeRabbit,
  PATRON_NOMBRE_RABBIT,
  PATRON_ROUTING_KEY,
  PATRON_ROUTING_KEY_PUBLICACION,
} from '../../core/models/rabbit.model';
import { RabbitAdminService } from '../../core/services/rabbit-admin.service';
import { SnackbarService } from '../../core/services/snackbar.service';
import { extraerMensajeError } from '../../core/utils/error.util';

/**
 * Panel de administración de RabbitMQ (solo ADMIN): colas, exchanges,
 * bindings, publicación de mensajes de prueba e inspección de DLQs.
 */
@Component({
  selector: 'app-rabbit-admin',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatTabsModule,
    MatTooltipModule,
  ],
  templateUrl: './rabbit-admin.component.html',
  styleUrl: './rabbit-admin.component.css',
})
export class RabbitAdminComponent implements OnInit {
  colas: ColaRabbit[] = [];
  exchanges: ExchangeRabbit[] = [];
  bindings: BindingRabbit[] = [];
  mensajes: MensajeRabbit[] = [];
  colaInspeccionada = '';
  cargando = false;
  error = '';

  readonly tiposExchange: CrearExchangeRequest['tipo'][] = ['direct', 'topic', 'fanout', 'headers'];
  readonly columnasColas = ['nombre', 'mensajes', 'consumidores', 'dlq', 'acciones'];
  readonly columnasExchanges = ['nombre', 'tipo', 'durable', 'acciones'];
  readonly columnasBindings = ['exchange', 'routingKey', 'cola', 'acciones'];

  readonly formCola = this.fb.group({
    nombre: ['', [Validators.required, Validators.pattern(PATRON_NOMBRE_RABBIT)]],
    durable: [true],
    ttlMensajesMs: [null as number | null, [Validators.min(1), Validators.max(604800000)]],
    maxMensajes: [null as number | null, [Validators.min(1), Validators.max(1000000)]],
    deadLetterExchange: ['', [Validators.pattern(PATRON_NOMBRE_RABBIT)]],
    deadLetterRoutingKey: ['', [Validators.pattern(PATRON_ROUTING_KEY)]],
  });

  readonly formExchange = this.fb.group({
    nombre: ['', [Validators.required, Validators.pattern(PATRON_NOMBRE_RABBIT)]],
    tipo: ['topic' as CrearExchangeRequest['tipo'], Validators.required],
    durable: [true],
  });

  readonly formBinding = this.fb.group({
    exchange: ['', Validators.required],
    cola: ['', Validators.required],
    routingKey: ['', [Validators.pattern(PATRON_ROUTING_KEY)]],
  });

  readonly formMensaje = this.fb.group({
    exchange: ['', Validators.required],
    routingKey: ['', [Validators.required, Validators.pattern(PATRON_ROUTING_KEY_PUBLICACION)]],
    payload: ['{\n  "mascotaId": 999,\n  "estado": "PERDIDA"\n}', Validators.required],
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly rabbit: RabbitAdminService,
    private readonly snackbar: SnackbarService,
  ) {}

  ngOnInit(): void {
    this.recargar();
  }

  recargar(): void {
    this.cargando = true;
    this.error = '';
    forkJoin({
      colas: this.rabbit.listarColas(),
      exchanges: this.rabbit.listarExchanges(),
      bindings: this.rabbit.listarBindings(),
    }).subscribe({
      next: ({ colas, exchanges, bindings }) => {
        this.colas = [...colas].sort((a, b) => a.nombre.localeCompare(b.nombre));
        this.exchanges = exchanges;
        this.bindings = bindings;
        this.cargando = false;
      },
      error: (err) => {
        this.error = extraerMensajeError(err, 'No se pudo consultar RabbitMQ');
        this.cargando = false;
      },
    });
  }

  dlqDe(cola: ColaRabbit): string {
    const dlq = cola.argumentos?.['x-dead-letter-routing-key'];
    return typeof dlq === 'string' ? dlq : '—';
  }

  esDlq(cola: ColaRabbit): boolean {
    return cola.nombre.endsWith('.dlq') || cola.nombre === 'q.no-entregados';
  }

  // ─── Colas ──────────────────────────────────────────────────────────────

  crearCola(): void {
    if (this.formCola.invalid) {
      this.formCola.markAllAsTouched();
      return;
    }
    const v = this.formCola.getRawValue();
    if (v.deadLetterRoutingKey && !v.deadLetterExchange) {
      this.snackbar.error('Para usar deadLetterRoutingKey debes indicar deadLetterExchange');
      return;
    }
    const req: CrearColaRequest = {
      nombre: (v.nombre ?? '').trim(),
      durable: v.durable ?? true,
      ttlMensajesMs: v.ttlMensajesMs || null,
      maxMensajes: v.maxMensajes || null,
      deadLetterExchange: v.deadLetterExchange || null,
      deadLetterRoutingKey: v.deadLetterRoutingKey || null,
    };
    this.rabbit.crearCola(req).subscribe({
      next: () => {
        this.snackbar.success(`Cola ${req.nombre} creada`);
        this.formCola.reset({ durable: true, nombre: '', deadLetterExchange: '', deadLetterRoutingKey: '' });
        this.recargar();
      },
      error: (err) => this.mostrarError(err, 'No se pudo crear la cola'),
    });
  }

  eliminarCola(cola: ColaRabbit): void {
    if (!confirm(`¿Eliminar la cola ${cola.nombre}? Se perderán sus ${cola.mensajes} mensajes.`)) return;
    this.rabbit.eliminarCola(cola.nombre).subscribe({
      next: () => {
        this.snackbar.success(`Cola ${cola.nombre} eliminada`);
        this.recargar();
      },
      error: (err) => this.mostrarError(err, 'No se pudo eliminar la cola'),
    });
  }

  purgarCola(cola: ColaRabbit): void {
    if (!confirm(`¿Vaciar la cola ${cola.nombre}?`)) return;
    this.rabbit.purgarCola(cola.nombre).subscribe({
      next: (r) => {
        this.snackbar.success(`${r.mensajesEliminados} mensaje(s) eliminados de ${cola.nombre}`);
        this.recargar();
      },
      error: (err) => this.mostrarError(err, 'No se pudo purgar la cola'),
    });
  }

  verMensajes(cola: ColaRabbit): void {
    this.colaInspeccionada = cola.nombre;
    this.rabbit.leerMensajes(cola.nombre, 10).subscribe({
      next: (m) => (this.mensajes = m),
      error: (err) => this.mostrarError(err, 'No se pudieron leer los mensajes'),
    });
  }

  motivoDlq(m: MensajeRabbit): string {
    const headers = m.propiedades?.['headers'] as Record<string, unknown> | undefined;
    const muertes = headers?.['x-death'] as Array<Record<string, unknown>> | undefined;
    if (!muertes?.length) return '';
    const d = muertes[0];
    return `${d['reason']} desde ${d['queue']}`;
  }

  json(valor: unknown): string {
    return typeof valor === 'string' ? valor : JSON.stringify(valor, null, 2);
  }

  // ─── Exchanges ──────────────────────────────────────────────────────────

  crearExchange(): void {
    if (this.formExchange.invalid) {
      this.formExchange.markAllAsTouched();
      return;
    }
    const v = this.formExchange.getRawValue();
    const req: CrearExchangeRequest = {
      nombre: (v.nombre ?? '').trim(),
      tipo: v.tipo ?? 'topic',
      durable: v.durable ?? true,
    };
    this.rabbit.crearExchange(req).subscribe({
      next: () => {
        this.snackbar.success(`Exchange ${req.nombre} creado`);
        this.formExchange.reset({ nombre: '', tipo: 'topic', durable: true });
        this.recargar();
      },
      error: (err) => this.mostrarError(err, 'No se pudo crear el exchange'),
    });
  }

  eliminarExchange(ex: ExchangeRabbit): void {
    if (!confirm(`¿Eliminar el exchange ${ex.nombre}?`)) return;
    this.rabbit.eliminarExchange(ex.nombre).subscribe({
      next: () => {
        this.snackbar.success(`Exchange ${ex.nombre} eliminado`);
        this.recargar();
      },
      error: (err) => this.mostrarError(err, 'No se pudo eliminar el exchange'),
    });
  }

  // ─── Bindings ───────────────────────────────────────────────────────────

  crearBinding(): void {
    if (this.formBinding.invalid) {
      this.formBinding.markAllAsTouched();
      return;
    }
    const v = this.formBinding.getRawValue();
    const req: BindingRabbit = { exchange: v.exchange ?? '', cola: v.cola ?? '', routingKey: v.routingKey ?? '' };
    this.rabbit.crearBinding(req).subscribe({
      next: () => {
        this.snackbar.success(`Binding ${req.exchange} → ${req.cola} creado`);
        this.formBinding.reset({ exchange: '', cola: '', routingKey: '' });
        this.recargar();
      },
      error: (err) => this.mostrarError(err, 'No se pudo crear el binding'),
    });
  }

  eliminarBinding(b: BindingRabbit): void {
    if (!confirm(`¿Eliminar el binding ${b.exchange} → ${b.cola} (${b.routingKey || 'sin routing key'})?`)) return;
    this.rabbit.eliminarBinding(b).subscribe({
      next: () => {
        this.snackbar.success('Binding eliminado');
        this.recargar();
      },
      error: (err) => this.mostrarError(err, 'No se pudo eliminar el binding'),
    });
  }

  // ─── Mensajes de prueba ─────────────────────────────────────────────────

  publicar(): void {
    if (this.formMensaje.invalid) {
      this.formMensaje.markAllAsTouched();
      return;
    }
    const v = this.formMensaje.getRawValue();
    let payload: Record<string, unknown>;
    try {
      const parsed = JSON.parse(v.payload ?? '');
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error();
      payload = parsed;
    } catch {
      this.snackbar.error('El payload debe ser un objeto JSON válido');
      return;
    }
    this.rabbit
      .publicarMensaje({ exchange: v.exchange ?? '', routingKey: v.routingKey ?? '', payload })
      .subscribe({
        next: () => {
          this.snackbar.success(`Mensaje publicado en ${v.exchange} con ${v.routingKey}`);
          setTimeout(() => this.recargar(), 800);
        },
        error: (err) => this.mostrarError(err, 'No se pudo publicar el mensaje'),
      });
  }

  private mostrarError(err: unknown, fallback: string): void {
    const detalle = (err as { error?: { detalles?: string[] } })?.error?.detalles;
    const mensaje = extraerMensajeError(err, fallback);
    this.snackbar.show(detalle?.length ? `${mensaje}: ${detalle.join('; ')}` : mensaje, 'error', 6000);
  }
}
