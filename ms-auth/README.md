# MS-Auth — Microservicio de Autenticación

**Puerto:** 8084
**Tecnología:** Spring Boot 3.2.5 + Spring Security + JWT (Auth0/RS256) + BCrypt + PostgreSQL

## Descripción

Microservicio que mantiene el perfil de los usuarios (nombre, teléfono, rol)
de la plataforma. La autenticación y emisión del JWT la realiza **Auth0**
(IDaaS, Authorization Code + PKCE); ms-auth **sincroniza por email** el
perfil local desde la identidad verificada del access token. Valida cada
token contra el **JWKS de Auth0** (firma RS256, issuer, audience y vigencia).

## Responsabilidades

- Sincronizar el perfil del usuario desde Auth0 por email (upsert en `/sync`)
- Consultar y actualizar el perfil (`GET/PUT /perfil`)
- Cambio de contraseña (legacy de la cuenta local) (`PUT /password`)
- Administración de usuarios (solo rol ADMIN): listar y eliminar
- Validar de forma independiente el JWT de Auth0 en cada petición

## Estructura

```
ms-auth/
├── src/main/java/cl/duocuc/sanosysalvos/auth/
│   ├── MsAuthApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java         # Spring Security sin sesión (stateless)
│   │   ├── JwtAuthenticationFilter.java# Filtro de validación de JWT
│   │   └── GlobalExceptionHandler.java
│   ├── controller/
│   │   └── AuthController.java         # /api/auth/**
│   ├── model/
│   │   ├── Usuario.java                # Entidad JPA
│   │   └── RolUsuario.java             # Enum de roles
│   ├── repository/
│   │   └── UsuarioRepository.java
│   └── service/
│       ├── AuthService.java            # Lógica de sincronización y perfiles
│       └── JwtService.java             # Validación JWT contra JWKS de Auth0
├── src/test/java/.../service/
│   └── AuthServiceTest.java
└── pom.xml
```

## Endpoints (todos autenticados salvo health)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/sync` | Sincroniza/crea el usuario local desde Auth0 |
| GET | `/api/auth/perfil` | Obtiene el perfil |
| PUT | `/api/auth/perfil` | Actualiza el perfil |
| PUT | `/api/auth/password` | Cambia la contraseña local |
| GET | `/api/auth/usuarios` | Lista usuarios (solo ADMIN) |
| DELETE | `/api/auth/usuarios/{id}` | Elimina usuario (solo ADMIN) |
| GET | `/api/auth/health` | Health check (público) |

## Requisitos

- Java 17
- Maven 3.8+
- PostgreSQL 16 (base de datos `db_auth`)
- Variables `AUTH0_DOMAIN` y `AUTH0_AUDIENCE` (podadas en `application.properties`)

## Ejecución local

```bash
cd ms-auth
mvn spring-boot:run
```

## Ejecutar pruebas

```bash
mvn test
```