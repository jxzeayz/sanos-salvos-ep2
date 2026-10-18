# MS-Auditoria — Microservicio de Auditoría

**Puerto:** 8087
**Tecnología:** Spring Boot 3.2.5 + RabbitMQ (consumer) + JPA + PostgreSQL

## Descripción

Microservicio que registra un log de todos los eventos del sistema. Consume desde la cola RabbitMQ `q.auditoria` que recibe todos los eventos publicados en el Topic Exchange `sanos-salvos.events` mediante el binding `*.*` (todos los routing keys). Expone una API REST para consultar los logs de auditoría, filtrable por servicio, evento y rango de fechas. **Acceso restringido a usuarios con rol ADMIN.**

## Patrones aplicados

- **Event-Driven (Consumer)**: `AuditEventConsumer` escucha todos los eventos del exchange de forma desacoplada
- **Repository**: acceso a datos mediante `AuditLogRepository`
- **Builder**: construcción de `AuditLog` con Lombok `@Builder`

## Flujo

```
Cualquier microservicio (ms-mascotas, ms-matching, ms-auth, etc.)
    └── publica evento en sanos-salvos.events (routing key: *.*)
            └── q.auditoria (binding: *.*)
                    └── AuditEventConsumer
                            └── AuditService.guardar()
                                    └── PostgreSQL
```

## Estructura

```
ms-auditoria/
├── src/main/java/com/sanosysalvos/msauditoria/
│   ├── MsAuditoriaApplication.java
│   ├── config/
│   │   ├── RabbitMQConfig.java            # Exchange, queue y binding
│   │   ├── GlobalExceptionHandler.java    # Manejo centralizado de errores
│   │   └── GlobalCorsConfig.java          # CORS
│   ├── consumer/
│   │   └── AuditEventConsumer.java        # Listener RabbitMQ (todos los eventos)
│   ├── controller/
│   │   └── AuditController.java           # REST endpoints (solo ADMIN)
│   ├── model/
│   │   └── AuditLog.java                  # Entidad JPA
│   ├── repository/
│   │   └── AuditLogRepository.java
│   └── service/
│       └── AuditService.java
├── src/test/java/.../consumer/
│   └── AuditEventConsumerTest.java
└── pom.xml
```

## Endpoints

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| GET | `/api/audit/logs` | Listar todos los logs | ADMIN |
| GET | `/api/audit/logs?servicio={nombre}` | Filtrar por servicio origen | ADMIN |
| GET | `/api/audit/logs?evento={nombre}` | Filtrar por tipo de evento | ADMIN |

### Ejemplo de respuesta

```json
{
  "id": 1,
  "evento": "mascota.registrada",
  "servicioOrigen": "ms-mascotas",
  "payload": "{\"mascotaId\":1,\"usuarioId\":1,\"nombre\":\"Firulais\",...}",
  "fechaEvento": "2026-06-18T10:30:00"
}
```

## Requisitos

- Java 17
- Maven 3.8+
- PostgreSQL 16 (base de datos `db_auditoria`)
- RabbitMQ 3.13

## Ejecución local

```bash
cd ms-auditoria
mvn spring-boot:run
```

## Variables de entorno (Docker)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-auditoria:5432/db_auditoria
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
SPRING_RABBITMQ_EXCHANGE=sanos-salvos.events
```

## Ejecutar pruebas

```bash
mvn test
```
