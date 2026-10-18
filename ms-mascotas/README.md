# MS-Mascotas — Microservicio de Gestión de Mascotas

**Puerto:** 8081
**Tecnología:** Spring Boot 3.2.5 + JPA + RabbitMQ + Resilience4j + PostgreSQL

## Descripción

Microservicio encargado del CRUD de mascotas. Cada vez que se registra una mascota, publica un evento asíncrono en RabbitMQ para que el microservicio de matching procese las coincidencias. Aplica Circuit Breaker para tolerar fallos en la mensajería sin interrumpir el registro.

## Patrones aplicados

- **Repository**: acceso a datos desacoplado mediante `MascotaRepository`
- **Builder**: construcción de entidades `Mascota` con Lombok `@Builder`
- **Publisher (Event-Driven)**: `MascotaEventPublisher` publica eventos al exchange de RabbitMQ
- **Circuit Breaker**: Resilience4j protege la publicación del evento

## Estructura

```
ms-mascotas/
├── src/main/java/cl/duocuc/sanosysalvos/mascotas/
│   ├── MsMascotasApplication.java
│   ├── config/
│   │   └── RabbitMQConfig.java         # Exchange, queues y bindings
│   ├── controller/
│   │   └── MascotaController.java      # REST endpoints
│   ├── dto/
│   │   └── MascotaRequest.java
│   ├── event/
│   │   └── MascotaEventPublisher.java  # Publicación al topic exchange
│   ├── model/
│   │   ├── Mascota.java
│   │   ├── EstadoMascota.java          # PERDIDA, ENCONTRADA, REUNIFICADA
│   │   ├── Reporte.java
│   │   └── TipoReporte.java
│   ├── repository/
│   │   ├── MascotaRepository.java
│   │   └── ReporteRepository.java
│   └── service/
│       └── MascotaService.java
├── src/test/java/.../service/
│   └── MascotaServiceTest.java
└── pom.xml
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/mascotas` | Registrar mascota (dispara evento RabbitMQ) |
| GET | `/mascotas` | Listar todas las mascotas |
| GET | `/mascotas/{id}` | Obtener mascota por ID |
| GET | `/mascotas/estado/{estado}` | Filtrar por estado |
| GET | `/mascotas/usuario/{usuarioId}` | Mascotas de un usuario |
| PUT | `/mascotas/{id}/estado` | Actualizar estado de mascota |

### Ejemplo de registro

```json
POST /mascotas
{
  "nombre": "Firulais",
  "especie": "PERRO",
  "raza": "Labrador",
  "color": "dorado",
  "tamano": "GRANDE",
  "estado": "PERDIDA",
  "latitud": -33.4569,
  "longitud": -70.6483,
  "usuarioId": 1
}
```

## Requisitos

- Java 17
- Maven 3.8+
- PostgreSQL 16 (base de datos `db_mascotas`)
- RabbitMQ 3.13

## Ejecución local

```bash
cd ms-mascotas
mvn spring-boot:run
```

## Variables de entorno (Docker)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-mascotas:5432/db_mascotas
SPRING_RABBITMQ_HOST=rabbitmq
```

## Ejecutar pruebas

```bash
mvn test
```
