# Plan de Branching — Git Flow
## Proyecto Sanos y Salvos — DSY1106 Desarrollo Fullstack III
**Estudiantes:** Julio Soto | Armando Calderón  
**Docente:** Jorge Canales Soto  
**Fecha:** Mayo 2026

---

## 1. Estrategia adoptada: Git Flow simplificado

El proyecto implementa una versión simplificada de **Git Flow**, con dos ramas permanentes y ramas de feature por componente.

### Estructura de ramas

```
main
  └── develop
        ├── feature/ms-auth
        ├── feature/ms-mascotas
        ├── feature/ms-geolocalizacion
        ├── feature/ms-matching
        ├── feature/bff-web
        └── feature/frontend
```

---

## 2. Ramas permanentes

### `main`
- Contiene el código de producción estable.
- Solo recibe merges desde `develop` cuando se completa una versión.
- Cada merge a `main` representa un **release** con tag de versión.
- **Nadie hace commits directos a `main`.**

### `develop`
- Rama de integración continua.
- Integra el trabajo de todas las ramas `feature/*`.
- Siempre debe estar en estado funcional (compilable y ejecutable con Docker Compose).
- Los merges a `develop` se realizan desde cada `feature/*` al completarla.

---

## 3. Ramas de feature

Cada componente del sistema se desarrolló en su propia rama de feature, siguiendo la convención: `feature/<nombre-componente>`.

| Rama | Componente | Responsable | Commit de merge |
|---|---|---|---|
| `feature/ms-auth` | Microservicio de autenticación | Equipo | `0521ee6` |
| `feature/ms-mascotas` | Microservicio de mascotas | Equipo | `e941cb2` — `032ccf4` |
| `feature/ms-geolocalizacion` | Microservicio de geolocalización | Equipo | `f91f8c9` — `032ccf4` |
| `feature/ms-matching` | Motor de coincidencias | Equipo | `e941cb2` — `7d37cf1` |
| `feature/bff-web` | Backend For Frontend | Equipo | `f0879cf` — `a0dd815` |
| `feature/frontend` | Aplicación React | Equipo | `9006514` — `db4d2fa` |

---

## 4. Flujo de trabajo

### Crear una nueva feature

```bash
# Desde develop actualizado
git checkout develop
git pull origin develop
git checkout -b feature/nombre-componente
```

### Desarrollar y commitear

```bash
# Commits durante el desarrollo
git add .
git commit -m "feat(ms-auth): agregar endpoint de registro con validación"
```

**Convención de mensajes de commit (Conventional Commits):**

| Prefijo | Uso |
|---|---|
| `feat(scope)` | Nueva funcionalidad |
| `fix(scope)` | Corrección de bug |
| `refactor(scope)` | Refactorización sin cambio de comportamiento |
| `release:` | Versión de release |
| `merge:` | Integración de ramas |

### Integrar la feature a develop

```bash
git checkout develop
git merge feature/nombre-componente --no-ff
git push origin develop
```

### Crear un release a main

```bash
git checkout main
git merge develop --no-ff
git tag -a v1.2.0 -m "release: proyecto completo con Docker v1.2.0"
git push origin main --tags
```

---

## 5. Historial de releases

| Tag | Commit | Descripción |
|---|---|---|
| v1.0.0 | `c0e67fd` | Backend completo — todos los microservicios integrados |
| v1.1.0 | `97f48bc` | Frontend React integrado |
| v1.2.0 | `ba0793b` | Proyecto completo con Docker |

---

## 6. Evidencia de merges en el repositorio

El historial de Git refleja la estrategia de branching con los siguientes merges documentados:

```
e1c20da fix(bff-web): usar variables de entorno Docker
ba0793b release: proyecto completo con Docker v1.2.0
a76d8e1 merge: actualizar frontend con Dockerfile y compose completo
369025f feat(docker): agregar Dockerfile frontend con nginx y healthchecks
97f48bc release: frontend React integrado v1.1.0
db4d2fa merge: integrar feature/frontend en develop
9006514 feat(frontend): agregar aplicacion React con componentes NPM
c0e67fd release: backend completo v1.0.0
a0dd815 merge: integrar feature/bff-web en develop
f0879cf feat(bff-web): agregar Backend For Frontend para React
7d37cf1 merge: integrar feature/ms-matching en develop
e941cb2 feat(ms-matching): agregar motor de coincidencias entre mascotas
032ccf4 merge: integrar feature/ms-geolocalizacion en develop
f91f8c9 feat(ms-geolocalizacion): agregar microservicio de geolocalizacion
0521ee6 merge: integrar feature/ms-mascotas en develop
```

---

## 7. Gestión de conflictos

Durante el desarrollo, los conflictos de merge se resolvieron manualmente editando los archivos afectados y realizando un commit de merge explícito. Los principales conflictos se produjeron en:

- **`docker-compose.yml`**: al integrar cada nueva feature, se agregaban nuevos servicios. Se resolvió aceptando ambos cambios y unificando el archivo.
- **`application.properties`**: configuraciones de puerto distintas por microservicio. Se resolvió manteniendo los valores de la feature entrante.

En todos los casos, después de resolver el conflicto:

```bash
git add <archivo-conflictivo>
git commit -m "merge: resolver conflicto en docker-compose.yml"
```

---

## 8. Ventajas de la estrategia adoptada

| Ventaja | Descripción |
|---|---|
| **Aislamiento** | Cada componente se desarrolla en su rama sin interferir con el trabajo de otros integrantes |
| **Historial limpio** | Los merges `--no-ff` mantienen un historial que muestra claramente qué feature se integró y cuándo |
| **Estabilidad de main** | `main` solo recibe código probado y estable, los releases son reproducibles |
| **Colaboración paralela** | Dos integrantes pueden trabajar simultáneamente en features distintas sin conflictos frecuentes |
| **Trazabilidad** | Cada commit de merge en `develop` es el punto de integración de una feature completa |

---

## 9. Repositorio GitHub

**URL del repositorio:** https://github.com/jxzeayz/sanos-y-salvos

Para clonar y verificar el historial de ramas:

```bash
git clone https://github.com/jxzeayz/sanos-y-salvos.git
cd sanos-y-salvos
git log --oneline --graph --all
```
