# BFF-Web — Backend For Frontend

**Patrón:** Backend For Frontend (BFF)
**Puerto:** 8080 (interno, solo accesible vía Kong)
**Tecnología:** Spring Boot 3.2.5 + WebFlux + JWT (Auth0/RS256)

## Descripción

El BFF-Web actúa como punto de entrada único del frontend Angular hacia los
microservicios internos. Valida los tokens JWT de Auth0 (firma RS256 contra
el JWKS, issuer, audience y vigencia) en cada request y reenvía la petición
al microservicio correspondiente usando `WebClient` (reactivo), enriqueciendo
las llamadas con los datos del token (`usuarioId`, `rol`).

> **Importante:** el puerto 8080 no se publica al host. Todas las rutas se
> consumen a través del API Manager (Kong, `http://localhost:8000/bff/*`),
> que también exige el JWT de Auth0, de modo que ninguna ruta interna eluda
> la validación del gateway.

## Responsabilidades

- Validar el token JWT de Auth0 antes de reenviar al microservicio
- Proxy de autenticación → `ms-auth` (:8084)
- Proxy de mascotas → `ms-mascotas` (:8081)
- Proxy de geolocalización → `ms-geolocalizacion` (:8082)
- Proxy de coincidencias/contacto → `ms-matching` (:8083)
- Proxy de notificaciones → `ms-notificaciones` (:8085)
- Proxy de archivos → `ms-archivos` (:8086)
- Proxy de auditoría → `ms-auditoria` (:8087)
- Ocultar la topología interna de microservicios al frontend

## Estructura

```
bff-web/
├── src/main/java/cl/duocuc/sanosysalvos/bff/
│   ├── BffWebApplication.java
│   ├── config/
│   │   ├── JwtValidator.java        # Valida tokens de entrada (RS256/JWKS)
│   │   ├── Auth0JwkStore.java       # Cache de claves públicas de Auth0
│   │   ├── Errores.java             # Respuestas JSON 401/403
│   │   └── WebClientConfig.java     # Clientes reactivos hacia microservicios
│   └── controller/
│       ├── AuthProxyController.java    # /bff/auth/**
│       ├── MascotaProxyController.java # /bff/mascotas/**
│       ├── ArchivoProxyController.java # /bff/archivos/**
│       └── ... (contacto, notificaciones, auditoría)
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

## Endpoints expuestos (todos requieren token de Auth0)

| Área | Rutas | Destino |
|---|---|---|
| Auth | `/bff/auth/perfil`, `/bff/auth/password`, `/bff/auth/sync`, `/bff/auth/usuarios` | ms-auth |
| Mascotas | `/bff/mascotas/**` | ms-mascotas, ms-matching, ms-geo |
| Archivos | `/bff/archivos/**` (subir, listar, descargar, eliminar) | ms-archivos |
| Notificaciones | `/bff/notificaciones/**` | ms-notificaciones |
| Auditoría | `/bff/auditoria/**` | ms-auditoria |

## Requisitos

- Java 17
- Maven 3.8+

## Ejecución local

```bash
cd bff-web
mvn spring-boot:run
```

## Variables de entorno (Docker)

```properties
SERVICES_AUTH_URL=http://ms-auth:8084
SERVICES_MASCOTAS_URL=http://ms-mascotas:8081
SERVICES_GEO_URL=http://ms-geolocalizacion:8082
SERVICES_MATCHING_URL=http://ms-matching:8083
SERVICES_NOTIFICACIONES_URL=http://ms-notificaciones:8085
SERVICES_ARCHIVOS_URL=http://ms-archivos:8086
SERVICES_AUDITORIA_URL=http://ms-auditoria:8087
AUTH0_DOMAIN=dev-nn2816yh03z0o4fk.us.auth0.com
AUTH0_AUDIENCE=https://sanosysalvos/api
```

## Ejecutar pruebas

```bash
mvn test
```