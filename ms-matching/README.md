# MS-Matching — Microservicio de Motor de Coincidencias

**Puerto:** 8083
**Tecnología:** Spring Boot 3.2.5 + RabbitMQ (consumer) + JPA + PostgreSQL

## Descripción

Microservicio que implementa el motor de detección de coincidencias entre mascotas perdidas y encontradas. Se activa de forma **asíncrona** cuando `ms-mascotas` publica un evento en RabbitMQ. Aplica un algoritmo de scoring ponderado que evalúa especie, raza, color, tamaño, ubicación y fecha del reporte.

## Patrones aplicados

- **Strategy**: `MatchingAlgorithm` encapsula el algoritmo de scoring, permitiendo reemplazarlo sin modificar el servicio
- **Event-Driven (Consumer)**: `MascotaEventConsumer` escucha la cola RabbitMQ de forma desacoplada
- **Repository**: acceso a datos mediante `MascotaSnapshotRepository` y `CoincidenciaRepository`
- **Builder**: construcción de `Coincidencia` con Lombok `@Builder`

## Algoritmo de scoring

| Criterio | Peso | Descripción |
|---|---|---|
| Especie | 0.30 (excluyente) | Si no coincide → score = 0.0 |
| Raza | 0.25 | Coincidencia exacta o valor parcial si es nula |
| Color | 0.20 | Exacta (1.0), parcial (0.6), diferente (0.0) |
| Tamaño | 0.10 | Exacta o parcial si es nulo |
| Ubicación | 0.10 | Decreciente en radio de 10 km (Haversine) |
| Fecha | 0.05 | Decreciente en ventana de 30 días |

**Umbral mínimo configurable:** `matching.score-minimo=0.60` (60%)

## Estructura

```
ms-matching/
├── src/main/java/cl/duocuc/sanosysalvos/matching/
│   ├── MsMatchingApplication.java
│   ├── config/
│   │   └── RabbitMQConfig.java
│   ├── consumer/
│   │   └── MascotaEventConsumer.java   # Listener RabbitMQ
│   ├── controller/
│   │   └── MatchingController.java     # REST para consultar coincidencias
│   ├── model/
│   │   ├── Coincidencia.java
│   │   ├── EstadoCoincidencia.java     # PENDIENTE, CONFIRMADA, RECHAZADA
│   │   └── MascotaSnapshot.java        # Copia local del evento recibido
│   ├── repository/
│   │   ├── CoincidenciaRepository.java
│   │   └── MascotaSnapshotRepository.java
│   └── service/
│       ├── MatchingAlgorithm.java      # Patrón Strategy
│       └── MatchingService.java
├── src/test/java/.../service/
│   ├── MatchingAlgorithmTest.java
│   └── MatchingServiceTest.java
└── pom.xml
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/matching/coincidencias` | Listar todas las coincidencias |
| GET | `/matching/coincidencias/mascota/{id}` | Coincidencias de una mascota |
| PUT | `/matching/coincidencias/{id}/estado` | Confirmar o rechazar coincidencia |

## Requisitos

- Java 17
- Maven 3.8+
- PostgreSQL 16 (base de datos `db_matching`)
- RabbitMQ 3.13

## Ejecución local

```bash
cd ms-matching
mvn spring-boot:run
```

## Variables de entorno (Docker)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-matching:5432/db_matching
SPRING_RABBITMQ_HOST=rabbitmq
```

## Ejecutar pruebas

```bash
mvn test
```
