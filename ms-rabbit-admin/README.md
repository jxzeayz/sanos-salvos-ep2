# ms-rabbit-admin

Microservicio administrador de RabbitMQ del sistema **Sanos y Salvos**.
Permite crear, consultar y eliminar **colas, exchanges y bindings**, publicar
mensajes de prueba e inspeccionar DLQs sin consumir sus mensajes.

- Spring Boot 3.2 · Spring AMQP (`AmqpAdmin` / `RabbitAdmin`) · Spring Security · Bean Validation
- Puerto **8088**
- Seguridad: JWT de Auth0 (RS256) con rol **ADMIN** → sin token `401`, sin rol `403`
- Acceso: Kong `/api/rabbit/**` · BFF `/bff/rabbit/**` · Frontend *Admin → Administrar RabbitMQ*

## Estructura

```
config/      RabbitAdminConfig (RabbitTemplate JSON, RestClient), RabbitAdminProperties
controller/  RabbitAdminController   → solo delega en el servicio
service/     RabbitAdminService      → TODA la lógica de administración del broker
client/      RabbitManagementClient  → API HTTP de management (listados y lectura de mensajes)
dto/         CrearColaRequest, CrearExchangeRequest, BindingRequest, PublicarMensajeRequest, Respuestas
exception/   GlobalExceptionHandler + excepciones de dominio (404, 409, 403, 503)
security/    filtro JWT de Auth0 y SecurityConfig
```

## Endpoints

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/rabbit/queues` | Lista colas (mensajes, consumidores, argumentos) | 200 |
| GET | `/api/rabbit/queues/{nombre}` | Detalle de una cola | 200 / 404 |
| POST | `/api/rabbit/queues` | Crea una cola | 201 / 400 / 409 |
| DELETE | `/api/rabbit/queues/{nombre}?ifUnused=&ifEmpty=` | Elimina una cola | 204 / 403 / 404 / 409 |
| GET | `/api/rabbit/queues/{nombre}/messages?cantidad=10` | Lee mensajes sin consumirlos | 200 |
| DELETE | `/api/rabbit/queues/{nombre}/messages` | Vacía (purga) la cola | 200 |
| GET | `/api/rabbit/exchanges` | Lista exchanges (sin los internos `amq.*`) | 200 |
| POST | `/api/rabbit/exchanges` | Crea un exchange | 201 / 400 / 409 |
| DELETE | `/api/rabbit/exchanges/{nombre}` | Elimina un exchange | 204 / 403 / 404 |
| GET | `/api/rabbit/bindings` | Lista bindings exchange → cola | 200 |
| POST | `/api/rabbit/bindings` | Crea un binding | 201 / 400 / 404 |
| DELETE | `/api/rabbit/bindings?exchange=&cola=&routingKey=` | Elimina un binding | 204 / 400 / 403 / 404 |
| POST | `/api/rabbit/messages` | Publica un mensaje JSON de prueba | 202 / 400 / 404 |

### Ejemplos

```bash
TOKEN="<access token de Auth0 con rol ADMIN>"

# Crear una cola con DLQ y TTL
curl -X POST http://localhost:8000/api/rabbit/queues \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"q.demo","ttlMensajesMs":60000,"deadLetterExchange":"sanos-salvos.dlx","deadLetterRoutingKey":"q.demo.dlq"}'

# Crear un exchange
curl -X POST http://localhost:8000/api/rabbit/exchanges \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"demo.events","tipo":"topic"}'

# Crear un binding
curl -X POST http://localhost:8000/api/rabbit/bindings \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"exchange":"sanos-salvos.events","cola":"q.demo","routingKey":"mascota.#"}'

# Eliminar el binding y la cola
curl -X DELETE "http://localhost:8000/api/rabbit/bindings?exchange=sanos-salvos.events&cola=q.demo&routingKey=mascota.%23" \
  -H "Authorization: Bearer $TOKEN"
curl -X DELETE http://localhost:8000/api/rabbit/queues/q.demo -H "Authorization: Bearer $TOKEN"
```

### Errores

Todas las respuestas de error tienen el mismo formato:

```json
{
  "timestamp": "2026-10-17T16:40:00",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Datos de entrada inválidos",
  "detalles": ["nombre solo admite letras, números, '.', '_', '-' y ':' (máx. 255) y no puede comenzar con 'amq.'"]
}
```

## Validaciones

- Nombres de cola/exchange: obligatorios, `^[A-Za-z0-9._:-]{1,255}$`, no pueden empezar con `amq.` (reservado por RabbitMQ).
- Routing keys: letras, números, `.`, `_`, `-`, `:`, `*`, `#` (los comodines no se permiten al publicar).
- `ttlMensajesMs` entre 1 ms y 7 días; `maxMensajes` entre 1 y 1.000.000.
- `deadLetterRoutingKey` exige `deadLetterExchange`, y el exchange de dead-letter debe existir.
- Una cola exclusiva no puede ser durable.
- `tipo` de exchange: `direct`, `topic`, `fanout` o `headers` (sin distinguir mayúsculas).
- No se puede crear lo que ya existe (409) ni eliminar los recursos del sistema
  listados en `rabbit-admin.recursos-protegidos` (403).

## Ejecutar pruebas

```bash
mvn clean test
```
