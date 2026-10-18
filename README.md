# Sanos y Salvos — Plataforma de localización de mascotas perdidas

**DSY1107 — Evaluación Encargo EA1 | DuocUC**
**Estudiantes:** Armando Calderón
**Docente:** Jorge Canales Soto
**Repositorio:** https://github.com/jxzeayz/sanos-salvos-2

---

## Descripción

Sanos y Salvos es una plataforma centralizada para el registro, visualización y detección de coincidencias entre mascotas perdidas y encontradas. La solución integra ciudadanos, clínicas veterinarias, refugios y municipalidades bajo una arquitectura de microservicios desacoplada.

## Arquitectura

```
Cliente (Navegador → Frontend Angular 17)
    │  HTTPS
    ▼
Kong — API Manager (Gateway :8000)
    │  valida JWT (plugin jwt / RS256)
    │  /api/*          /api/*          /bff/*
    ├──────────────┬───────────────────┴──────────────► BFF-Web (:8080)
    │              │                                       │  valida JWT y agrega usuarioId/rol
    │              │                                       ▼
    ├── ms-auth (:8084)         ├── ms-geolocalizacion (:8082 PostGIS)
    ├── ms-mascotas (:8081)     └── ms-matching (:8083)
    ├── ms-notificaciones (:8085)   ms-archivos (:8086)   ms-auditoria (:8087)

Infraestructura:
  PostgreSQL/Neon (BD cloud), PostGIS, RabbitMQ, MinIO, Auth0 (IDaaS)
```

**Seguridad (JWT):**
- **Kong** aplica el plugin `jwt` de forma global: exige un access token firmado por **Auth0** (RS256 contra el JWKS del tenant), verifica `exp` y rechaza a quien no lo presente, tanto en `/bff/*` como en `/api/*`.
- **BFF-Web** vuelve a validar el token (firma, `iss`, `aud`, vigencia) y enriquece las peticiones con `usuarioId`/`rol`.
- **ms-auth, ms-mascotas y ms-archivos** revalidan de forma independiente el token contra el JWKS de Auth0 (firma + `iss` + `aud` + `exp`), al igual que el API Manager.
- **Los roles se leen de los claims** del token (`https://sanosysalvos/rol`), no de datos editables en el cliente.
- El login/register local legacy fue eliminado: la única vía de obtener un token válido es el IDaaS (Auth0).

**Equivalencias con la pauta:**
| Pauta | Implementación |
|---|---|
| MSAL | `@auth0/auth0-angular` (SPA — Authorization Code + PKCE) |
| Azure AD / Microsoft Entra | **Auth0** (IDaaS — dominio `dev-nn2816yh03z0o4fk.us.auth0.com`) |
| AWS API Gateway | **Kong** (API Manager DB-less) + **Konga** (panel) |
| EC2 | **Docker Compose** (despliegue local reproducible) |
| PostgreSQL | **Neon** (BD cloud, 7 bases + PostGIS) |

**Puertas de entrada:**
- **Kong (:8000)** — API Manager: expone el BFF y cada microservicio (`/api/*`) de forma individual, validando el JWT de Auth0 en todas las rutas (plugin `jwt`), con consola de administración `:8001` y **Konga** (`:8085`).
- **BFF-Web (:8080)** — Backend for Frontend: consume los microservicios y enriquece las peticiones con `usuarioId`/`rol` validando el JWT de **Auth0** (RS256/JWKS).

**Infraestructura de soporte:**
- PostgreSQL local o **Neon (cloud)** — 7 bases independientes
- PostGIS (geolocalización)
- RabbitMQ (mensajería asíncrona)
- MinIO (almacenamiento de imágenes)
- Auth0 — Identity Provider (IDaaS)

## Componentes

| Componente | Tecnología | Puerto | README |
|---|---|---|---|
| [frontend](./frontend) | Angular 17 + Material + Leaflet | 443/80 | [README](./frontend/README.md) |
| [bff-web](./bff-web) | Spring Boot 3.2 + WebFlux | 8080 | [README](./bff-web/README.md) |
| [ms-auth](./ms-auth) | Spring Boot 3.2 + Spring Security | 8084 | [README](./ms-auth/README.md) |
| [ms-mascotas](./ms-mascotas) | Spring Boot 3.2 + JPA + AMQP + Spring Security | 8081 | [README](./ms-mascotas/README.md) |
| [ms-geolocalizacion](./ms-geolocalizacion) | Spring Boot 3.2 + PostGIS | 8082 | [README](./ms-geolocalizacion/README.md) |
| [ms-matching](./ms-matching) | Spring Boot 3.2 + RabbitMQ | 8083 | [README](./ms-matching/README.md) |
| [ms-notificaciones](./ms-notificaciones) | Spring Boot 3.2 + SMTP/AMQP | 8085 | — |
| [ms-archivos](./ms-archivos) | Spring Boot 3.2 + MinIO + Spring Security | 8086 | — |
| [ms-auditoria](./ms-auditoria) | Spring Boot 3.2 + AOP | 8087 | — |
| [ms-rabbit-admin](./ms-rabbit-admin) | Spring Boot 3.2 + AMQP (RabbitAdmin) + Spring Security | 8088 | [README](./ms-rabbit-admin/README.md) |

## Inicio rápido

### Requisitos
- Docker Desktop 4.x
- Docker Compose v2
- Cuenta **Auth0** (variables `AUTH0_DOMAIN`, `AUTH0_AUDIENCE` en `.env`) y opcionalmente **Neon** para BD cloud

### Levantar todo el sistema

```bash
docker compose up --build
```

Esto levanta **todos los servicios** automáticamente en orden, con healthchecks.

| Servicio | URL |
|---|---|
| Frontend (Angular) | https://localhost |
| Kong (API Manager — proxy) | http://localhost:8000 |
| Kong Admin API | http://localhost:8001 |
| Konga (panel API Manager) | http://localhost:8085 |
| RabbitMQ Management — nodo 1 | http://localhost:15672 (guest/guest) |
| RabbitMQ Management — nodo 2 | http://localhost:15673 (guest/guest) |
| MinIO Console | http://localhost:9001 (minioadmin/minioadmin) |

> **BFF-Web** (`:8080`) no se publica al host: solo es accesible a través de
> Kong (`http://localhost:8000/bff/*`) con un token de Auth0 válido, de modo
> que ninguna ruta interna eluda la validación del API Manager.

### BD en la nube (Neon)

1. Crea un proyecto en [Neon](https://neon.tech) y crea **7 bases**: `db_auth`, `db_mascotas`, `db_geo`, `db_matching`, `db_notificaciones`, `db_archivos`, `db_auditoria`.
2. En `db_geo` ejecuta `CREATE EXTENSION IF NOT EXISTS postgis;` (Neon soporta PostGIS).
3. Completa las variables `NEON_DB_*` en `.env` con las URLs **JDBC** (`jdbc:postgresql://ep-...neon.tech/db_x?sslmode=require`).
4. `docker compose up --build` usará los microservicios contra Neon; el esquema se crea automáticamente (`ddl-auto=update`).

> Si las variables `NEON_DB_*` están vacías, los microservicios usan el Postgres local del compose.

## Mensajería asíncrona (RabbitMQ)

RabbitMQ corre como un **clúster de 2 nodos** (`rabbitmq1` y `rabbitmq2`) con las colas replicadas en ambos.
Todos los microservicios se comunican por el exchange topic `sanos-salvos.events`.
Cada consumidor confirma los mensajes de forma manual (ACK/NACK) y los mensajes
que no se pueden procesar terminan en su **DLQ** a través del exchange
`sanos-salvos.dlx`. El microservicio **ms-rabbit-admin** expone una API REST
(y una vista en el frontend, *Admin → Administrar RabbitMQ*) para crear y
eliminar colas, exchanges y bindings.

Detalle de la topología, la política de ACK y un guion de demostración en
[docs/mensajeria-rabbitmq.md](./docs/mensajeria-rabbitmq.md).

> **Importante al actualizar:** RabbitMQ ahora es un clúster (`rabbitmq1`, `rabbitmq2`) y las colas
> se declaran con argumentos de dead-letter. Elimina el contenedor antiguo (`docker rm -f rabbitmq`) y
> ejecuta `docker compose down` antes de `docker compose up --build`.

## Estrategia de branching

El proyecto usa **Git Flow** simplificado:

- `main` — versiones estables (releases)
- `develop` — integración continua
- `feature/*` — desarrollo de cada componente

Ver [docs/plan-branching.md](./docs/plan-branching.md) para detalle completo.

## Patrones de diseño utilizados

- **Repository** (todos los microservicios)
- **Builder** (entidades con Lombok)
- **BFF / Proxy** (bff-web)
- **Event-Driven / Publisher-Subscriber** (ms-mascotas → ms-matching/ms-geolocalizacion/ms-auditoria → ms-notificaciones vía RabbitMQ)
- **Dead Letter Channel** (DLQ por consumidor + cola de mensajes no entregados)
- **Circuit Breaker** (Resilience4j en ms-mascotas)
- **Strategy** (MatchingAlgorithm en ms-matching)

Ver [docs/analisis-patrones-arquetipos.md](./docs/analisis-patrones-arquetipos.md) para análisis completo.
