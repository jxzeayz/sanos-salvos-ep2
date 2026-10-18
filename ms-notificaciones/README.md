# MS-Notificaciones — Microservicio de Notificaciones

**Puerto:** 8085
**Tecnología:** Spring Boot 3.2.5 + RabbitMQ (consumer) + JPA + PostgreSQL + SMTP

## Descripción

Microservicio responsable de enviar alertas y notificaciones a los usuarios cuando se detecta una coincidencia. Consume eventos desde la cola RabbitMQ `q.notificaciones` (binding `coincidencia.*`) y almacena notificaciones en PostgreSQL. Envía notificaciones por Email (SMTP) y las expone vía REST para consumo del frontend.

## Patrones aplicados

- **Event-Driven (Consumer)**: `CoincidenciaEventConsumer` escucha la cola RabbitMQ de forma desacoplada
- **Repository**: acceso a datos mediante `NotificacionRepository`
- **Builder**: construcción de `Notificacion` con Lombok `@Builder`

## Flujo

```
ms-matching (publisher)
    └── publica evento "coincidencia.hallada" en sanos-salvos.events
            └── q.notificaciones (binding coincidencia.*)
                    └── CoincidenciaEventConsumer
                            └── NotificacionService.guardar()
                                    └── PostgreSQL + SMTP (email)
```

## Estructura

```
ms-notificaciones/
├── src/main/java/com/sanosysalvos/msnotificaciones/
│   ├── MsNotificacionesApplication.java
│   ├── config/
│   │   ├── RabbitMQConfig.java            # Exchange, queue y binding
│   │   ├── RabbitMQQueueConfig.java       # Configuración de queue name
│   │   ├── GlobalExceptionHandler.java    # Manejo centralizado de errores
│   │   └── GlobalCorsConfig.java          # CORS
│   ├── consumer/
│   │   └── CoincidenciaEventConsumer.java # Listener RabbitMQ
│   ├── controller/
│   │   └── NotificacionController.java    # REST endpoints
│   ├── model/
│   │   ├── Notificacion.java              # Entidad JPA
│   │   └── TipoNotificacion.java          # Enum: COINCIDENCIA, ALERTA, SISTEMA
│   ├── repository/
│   │   └── NotificacionRepository.java
│   └── service/
│       └── NotificacionService.java
├── src/test/java/.../service/
│   └── NotificacionServiceTest.java
└── pom.xml
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/notificaciones?usuarioId={id}` | Listar notificaciones de un usuario |
| GET | `/api/notificaciones/{id}` | Obtener notificación por ID |
| PATCH | `/api/notificaciones/{id}/leida` | Marcar como leída |
| GET | `/api/notificaciones/no-leidas/count?usuarioId={id}` | Contar no leídas |

### Ejemplo de respuesta

```json
{
  "id": 1,
  "tipo": "COINCIDENCIA",
  "titulo": "Nueva coincidencia detectada",
  "mensaje": "Se ha encontrado una nueva coincidencia para tu mascota",
  "leida": false,
  "enviadaEn": "2026-06-18T10:30:00",
  "usuarioId": 1
}
```

## Requisitos

- Java 17
- Maven 3.8+
- PostgreSQL 16 (base de datos `db_notificaciones`)
- RabbitMQ 3.13

## Ejecución local

```bash
cd ms-notificaciones
mvn spring-boot:run
```

## Variables de entorno (Docker)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-notificaciones:5432/db_notificaciones
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
SPRING_RABBITMQ_EXCHANGE=sanos-salvos.events
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=notificaciones@sanosysalvos.com
SPRING_MAIL_PASSWORD=tu_password_aqui
```

## Ejecutar pruebas

```bash
mvn test
```
