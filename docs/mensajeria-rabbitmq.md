# Mensajería asíncrona con RabbitMQ

Este documento describe cómo se comunican los microservicios de Sanos y Salvos
mediante RabbitMQ: qué publica cada uno, qué consume, cómo se confirman los
mensajes y qué pasa con los que fallan.

## 0. Clúster de RabbitMQ (2 nodos)

| Nodo | Contenedor | Consola |
|---|---|---|
| `rabbit@rabbitmq1` | `rabbitmq1` | http://localhost:15672 |
| `rabbit@rabbitmq2` | `rabbitmq2` | http://localhost:15673 |

- Ambos nodos comparten la misma **Erlang cookie** (`RABBITMQ_ERLANG_COOKIE`, por defecto
  `sanos-salvos-cluster-cookie`).
- El contenedor `rabbitmq-cluster-init` (se ejecuta una vez y termina) une `rabbit@rabbitmq2` al clúster
  de `rabbit@rabbitmq1` (`stop_app` → `reset` → `join_cluster` → `start_app`) y aplica la política
  **`ha-colas-sanos`** (`^q\.` → `ha-mode: all`, `ha-sync-mode: automatic`): cada cola y su DLQ se
  replican en los dos nodos. Los microservicios arrancan solo cuando este paso terminó bien.
- Los microservicios se conectan a ambos nodos con `SPRING_RABBITMQ_ADDRESSES=rabbitmq1:5672,rabbitmq2:5672`;
  si un nodo cae, Spring AMQP reconecta al otro automáticamente.

Verificación:

```bash
docker exec rabbitmq1 rabbitmqctl cluster_status        # Running Nodes: rabbit@rabbitmq1, rabbit@rabbitmq2
docker exec rabbitmq1 rabbitmqctl list_policies         # ha-colas-sanos
docker logs rabbitmq-cluster-init                       # política aplicada + estado del clúster
```

Prueba de tolerancia a fallos: `docker stop rabbitmq2` → la consola de `rabbitmq1` muestra el nodo 2 caído,
las colas siguen disponibles y el sistema sigue funcionando; `docker start rabbitmq2` → el nodo vuelve a
unirse y las réplicas se sincronizan.

## 1. Topología

| Recurso | Tipo | Declarado en |
|---|---|---|
| `sanos-salvos.events` | exchange **topic** (durable) | todos los microservicios con RabbitMQ |
| `sanos-salvos.dlx` | exchange **direct** de dead-letter (durable) | todos los microservicios con RabbitMQ |
| `q.matching` → DLQ `q.matching.dlq` | cola durable | ms-matching |
| `q.notificaciones` → DLQ `q.notificaciones.dlq` | cola durable | ms-notificaciones |
| `q.geolocalizacion` → DLQ `q.geolocalizacion.dlq` | cola durable | ms-geolocalizacion |
| `q.auditoria` → DLQ `q.auditoria.dlq` | cola durable | ms-auditoria |
| `q.no-entregados` | cola durable | productores (mascotas, matching, geolocalizacion) |

### Flujo de eventos

```
ms-mascotas ──[mascota.perdida | mascota.encontrada | mascota.reunificada | mascota.eliminada]──▶ sanos-salvos.events
     ├──[mascota.*]──────────────────────────▶ q.matching ─────────▶ ms-matching
     │                                                                  └──[coincidencia.hallada]──▶ sanos-salvos.events
     │                                                                            ├──[coincidencia.*]──▶ q.notificaciones ──▶ ms-notificaciones
     │                                                                            └──[*.*]────────────▶ q.auditoria
     ├──[mascota.perdida | mascota.encontrada]─▶ q.geolocalizacion ─▶ ms-geolocalizacion
     │                                                                  └──[zona.actualizada]──▶ sanos-salvos.events ──[*.*]──▶ q.auditoria
     └──[*.*]────────────────────────────────▶ q.auditoria ────────▶ ms-auditoria

Rechazados:  q.<servicio> ──(NACK sin reencolar)──▶ sanos-salvos.dlx ──[q.<servicio>.dlq]──▶ q.<servicio>.dlq
Sin ruta:    productor ──(returned, mandatory=true)──▶ sanos-salvos.dlx ──[no-entregados]──▶ q.no-entregados
```

## 2. Configuración centralizada

Ningún nombre de cola, exchange o routing key está escrito en las clases de
negocio. Cada microservicio los define en su `application.properties` bajo el
prefijo `mensajeria` y los lee con una clase `@ConfigurationProperties`
(`MensajeriaProperties`). Ejemplo de ms-matching:

```properties
mensajeria.exchange=${RABBITMQ_EXCHANGE:sanos-salvos.events}
mensajeria.dlx=${RABBITMQ_DLX:sanos-salvos.dlx}
mensajeria.cola.nombre=q.matching
mensajeria.cola.dlq=q.matching.dlq
mensajeria.cola.binding-key=mascota.*
mensajeria.routing-keys.coincidencia-hallada=coincidencia.hallada
mensajeria.routing-keys.mascota-eliminada=mascota.eliminada
mensajeria.no-entregados.cola=q.no-entregados
mensajeria.no-entregados.routing-key=no-entregados
```

Los beans `Queue`, `Exchange` y `Binding` de cada caso de uso se declaran solo
en `config/RabbitMQConfig.java` (con un diagrama de la ruta en el Javadoc).
Los servicios de dominio no conocen RabbitMQ: llaman a un publicador
(`event/*Publisher`) y los consumidores viven en el paquete `consumer`.

| Microservicio | Configuración | Productor | Consumidor |
|---|---|---|---|
| ms-mascotas | `config/RabbitMQConfig`, `config/MensajeriaProperties` | `event/MascotaEventPublisher` | — |
| ms-matching | `config/RabbitMQConfig`, `config/MensajeriaProperties` | `event/CoincidenciaEventPublisher` | `consumer/MascotaEventConsumer` |
| ms-geolocalizacion | `config/RabbitMQConfig`, `config/MensajeriaProperties` | `event/ZonaEventPublisher` | `consumer/MascotaEventConsumer` |
| ms-notificaciones | `config/RabbitMQConfig`, `config/MensajeriaProperties` | — | `consumer/CoincidenciaEventConsumer` |
| ms-auditoria | `config/RabbitMQConfig`, `config/MensajeriaProperties` | — | `consumer/AuditEventConsumer` |
| ms-rabbit-admin | `config/RabbitAdminConfig`, `config/RabbitAdminProperties` | `RabbitAdminService#publicarMensaje` | — |

## 3. Confirmación de mensajes (ACK) y manejo de errores

Todos los consumidores usan `@RabbitListener(ackMode = "MANUAL")`, reciben el
`Message` crudo y el `Channel`, y deciden explícitamente:

| Situación | Tipo de error | Acción | Resultado |
|---|---|---|---|
| Procesado correctamente | — | `basicAck(tag, false)` | mensaje eliminado de la cola |
| JSON inválido, faltan campos obligatorios, tipos incorrectos | **No recuperable** (`EventoInvalidoException`) | `basicNack(tag, false, false)` | va a la **DLQ** + `log.error("[DLQ] ...")` |
| Error técnico (BD caída, timeout…) en la 1.ª entrega | **Recuperable** | `basicNack(tag, false, true)` | se **reencola** una vez + `log.warn("[REINTENTO] ...")` |
| Error técnico en una re-entrega (`redelivered = true`) | Recuperable agotado | `basicNack(tag, false, false)` | va a la **DLQ** + `log.error("[DLQ] ...")` |
| Evento que no aplica (p. ej. `mascota.eliminada` en matching, reporte sin coordenadas en geo) | — | `basicAck` | se descarta con `log.info` |

Además, en cada servicio:

```properties
spring.rabbitmq.listener.simple.acknowledge-mode=manual
spring.rabbitmq.listener.simple.default-requeue-rejected=false   # nunca reencolar en loop
spring.rabbitmq.listener.simple.prefetch=10
```

Los productores activan `publisher-confirm-type=correlated`,
`publisher-returns=true` y `mandatory=true`. `MensajesNoEntregadosHandler`
registra en el log todo mensaje que el broker no confirme o no pueda enrutar y
reenvía estos últimos a `q.no-entregados`.

## 4. Microservicio administrador (ms-rabbit-admin)

API REST protegida con JWT de Auth0 (solo rol **ADMIN**), expuesta en Kong
como `/api/rabbit/**` y en el BFF como `/bff/rabbit/**`. Toda la lógica está
en `RabbitAdminService` (usa `AmqpAdmin`/`RabbitAdmin`); el controlador solo
delega. Ver [ms-rabbit-admin/README.md](../ms-rabbit-admin/README.md).

## 5. Guion de demostración

1. `docker compose down && docker compose up --build` (recrea el clúster RabbitMQ con las colas nuevas).
2. Abrir **http://localhost:15672** (guest/guest):
   - *Overview → Nodes*: `rabbit@rabbitmq1` y `rabbit@rabbitmq2` en verde (clúster de 2 nodos).
   - *Exchanges*: `sanos-salvos.events` (**topic**) y `sanos-salvos.dlx` (**direct**).
   - *Queues*: las colas `q.*` y sus `q.*.dlq`, con la política `ha-colas-sanos` (réplica en ambos nodos).
   - *Admin → Policies*: política `ha-colas-sanos`.
3. **Flujo feliz:** en el frontend registrar una mascota PERDIDA y otra ENCONTRADA parecidas →
   aparece la notificación de coincidencia; en los logs de cada servicio se ve el ACK.
4. **Flujo DLQ:** en el frontend ir a *Admin → Administrar RabbitMQ → Publicar mensaje* y publicar en
   `sanos-salvos.events` con routing key `mascota.perdida` el payload `{"mascotaId": 999, "estado": "PERDIDA"}`
   (sin `usuarioId`).
   - `docker compose logs ms-matching` → `[DLQ] Evento inválido descartado de matching ...`
   - En la pestaña *Colas*, `q.matching.dlq` y `q.geolocalizacion.dlq` muestran 1 mensaje; con el
     botón 👁 se ve el payload y el motivo (`rejected desde q.matching`).
5. **Administración:** crear la cola `q.demo` con DLX `sanos-salvos.dlx`, crear el binding
   `sanos-salvos.events --[mascota.#]--> q.demo`, registrar otra mascota y ver que `q.demo` recibe una copia;
   luego eliminar el binding y la cola.
6. **Tolerancia a fallos del clúster:** `docker stop rabbitmq2`, registrar otra mascota (el flujo sigue
   funcionando) y `docker start rabbitmq2`.
7. **Validaciones:** intentar crear una cola con nombre vacío, con espacios o que empiece con `amq.`
   (400), crear una que ya existe (409) o eliminar `q.matching` (403, recurso protegido).
