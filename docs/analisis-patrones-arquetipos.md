# Análisis de Patrones de Diseño y Arquetipos Maven
## Proyecto Sanos y Salvos — DSY1106 Desarrollo Fullstack III
**Estudiantes:** Julio Soto | Armando Calderón  
**Docente:** Jorge Canales Soto  
**Fecha:** Mayo 2026

---

## 1. Introducción

Este documento describe los patrones de diseño implementados en los componentes frontend y backend de la plataforma Sanos y Salvos, así como los arquetipos Maven utilizados como base de los microservicios. Para cada patrón se justifica su elección en relación al problema que resuelve dentro del dominio de la aplicación.

---

## 2. Arquetipos Maven utilizados

Todos los componentes backend fueron generados usando el arquetipo **`maven-archetype-quickstart`** de Maven, con Spring Boot como parent POM:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>
```

### Componentes generados

| Componente | groupId | artifactId | Arquetipo base |
|---|---|---|---|
| BFF-Web | cl.duocuc.sanosysalvos | bff-web | maven-archetype-quickstart + spring-boot-starter-web + webflux |
| MS-Auth | cl.duocuc.sanosysalvos | ms-auth | maven-archetype-quickstart + spring-boot-starter-security |
| MS-Mascotas | cl.duocuc.sanosysalvos | ms-mascotas | maven-archetype-quickstart + spring-boot-starter-amqp |
| MS-Geolocalizacion | cl.duocuc.sanosysalvos | ms-geolocalizacion | maven-archetype-quickstart + hibernate-spatial |
| MS-Matching | cl.duocuc.sanosysalvos | ms-matching | maven-archetype-quickstart + spring-boot-starter-amqp |

### Cómo generar un nuevo microservicio desde el arquetipo

```bash
mvn archetype:generate \
  -DgroupId=cl.duocuc.sanosysalvos \
  -DartifactId=ms-nuevo \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.4 \
  -DinteractiveMode=false
```

Luego agregar el parent Spring Boot al `pom.xml` generado y las dependencias necesarias.

---

## 3. Patrones de diseño en el Backend

### 3.1 Patrón Repository

**Componentes que lo aplican:** ms-auth, ms-mascotas, ms-geolocalizacion, ms-matching

**Descripción:**  
El patrón Repository abstrae la capa de acceso a datos detrás de una interfaz, desacoplando la lógica de negocio de la tecnología de persistencia (JPA/Hibernate). Cada microservicio define su propio repositorio que extiende `JpaRepository`.

**Ejemplo — ms-auth:**
```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

**Justificación:**  
En un sistema distribuido como Sanos y Salvos, donde cada microservicio tiene su propia base de datos, el patrón Repository garantiza que la lógica de negocio (AuthService, MascotaService) no depende directamente de JPA. Si en el futuro se reemplaza PostgreSQL por otra tecnología de persistencia, el cambio queda confinado al repositorio sin afectar al servicio.

---

### 3.2 Patrón Builder

**Componentes que lo aplican:** ms-auth, ms-mascotas, ms-geolocalizacion, ms-matching

**Descripción:**  
El patrón Builder permite construir objetos complejos paso a paso. Se implementa mediante la anotación Lombok `@Builder` en todas las entidades JPA y DTOs de respuesta.

**Ejemplo — ms-mascotas:**
```java
Mascota mascota = Mascota.builder()
    .nombre(req.getNombre())
    .especie(req.getEspecie())
    .raza(req.getRaza())
    .color(req.getColor())
    .estado(req.getEstado())
    .latitud(req.getLatitud())
    .longitud(req.getLongitud())
    .usuarioId(req.getUsuarioId())
    .build();
```

**Justificación:**  
Las entidades del dominio (Mascota, Usuario, Coincidencia) tienen múltiples atributos opcionales. Sin el patrón Builder, el código de construcción usaría constructores con muchos parámetros o setters explícitos, aumentando el riesgo de errores y disminuyendo la legibilidad. Builder permite construir objetos válidos y expresivos en una sola cadena fluida.

---

### 3.3 Patrón Backend For Frontend (BFF)

**Componente:** bff-web

**Descripción:**  
El BFF es un servidor intermedio diseñado específicamente para las necesidades del cliente React. Valida el token JWT y reenvía la solicitud al microservicio interno correspondiente usando WebClient reactivo.

**Flujo:**
```
React App → [Bearer Token] → BFF-Web → [JWT válido] → ms-mascotas
                                      → [JWT inválido] → 401 Unauthorized
```

**Ejemplo — MascotaProxyController:**
```java
@GetMapping
public Mono<ResponseEntity<String>> listar(@RequestHeader("Authorization") String auth) {
    return mascotasClient.get()
        .uri("/mascotas")
        .header("Authorization", auth)
        .retrieve()
        .toEntity(String.class);
}
```

**Justificación:**  
Sin el BFF, el frontend debería conocer las URLs internas de cada microservicio y manejar autenticación con cada uno por separado. El BFF centraliza estas responsabilidades, oculta la topología interna y simplifica el cliente React a una sola URL base (`http://localhost:8080/api`).

---

### 3.4 Patrón Event-Driven / Publisher-Subscriber

**Componentes:** ms-mascotas (publisher) → ms-matching (subscriber)

**Descripción:**  
Cuando se registra una mascota, `ms-mascotas` publica un evento en el Topic Exchange de RabbitMQ (`sanos-salvos.events`). El `ms-matching` consume este evento de forma asíncrona y ejecuta el motor de coincidencias sin bloquear la respuesta al usuario.

**Flujo:**
```
POST /mascotas
    └── MascotaService.registrar()
        └── MascotaEventPublisher → RabbitMQ Exchange
                                        └── MascotaEventConsumer (ms-matching)
                                            └── MatchingService.procesarNuevaMascota()
```

**Justificación:**  
El procesamiento de coincidencias puede involucrar múltiples comparaciones costosas. Si se realizara de forma síncrona, el endpoint `POST /mascotas` tardaría varios segundos en responder. El patrón Event-Driven desacopla temporalmente el registro (respuesta inmediata al usuario) del matching (procesado en segundo plano), mejorando significativamente la experiencia de usuario.

---

### 3.5 Patrón Circuit Breaker

**Componente:** ms-mascotas (Resilience4j)

**Descripción:**  
El Circuit Breaker protege la publicación del evento RabbitMQ. Si el servicio de mensajería está temporalmente no disponible, el circuito se "abre" y las siguientes llamadas fallan rápidamente sin esperar timeout, ejecutando el método `publishFallback`.

**Ejemplo:**
```java
@CircuitBreaker(name = "matching-service", fallbackMethod = "publishFallback")
private void publishEventSafe(Mascota mascota) {
    eventPublisher.publishMascotaRegistrada(mascota);
}

private void publishFallback(Mascota mascota, Exception ex) {
    log.warn("Circuit breaker activo: no se pudo publicar evento para mascota {}.", mascota.getId());
}
```

**Justificación:**  
Sin Circuit Breaker, un fallo de RabbitMQ haría que cada `POST /mascotas` esperara el timeout completo antes de fallar, degradando toda la experiencia de registro. Con Circuit Breaker, el sistema continúa aceptando registros aunque el matching no funcione temporalmente.

---

### 3.6 Patrón Strategy

**Componente:** ms-matching (MatchingAlgorithm)

**Descripción:**  
El algoritmo de scoring está encapsulado en la clase `MatchingAlgorithm`, que implementa un comportamiento intercambiable. El `MatchingService` lo consume como una dependencia inyectada, sin conocer los detalles del cálculo.

**Justificación:**  
Si en el futuro se desea implementar un algoritmo diferente (por ejemplo, basado en machine learning), basta con crear una nueva implementación de `MatchingAlgorithm` e inyectarla, sin modificar `MatchingService`. El patrón Strategy garantiza que el motor de coincidencias sea extensible y testeable de forma aislada.

---

## 4. Patrones de diseño en el Frontend

### 4.1 Patrón Context / Provider

**Componente:** AuthContext.jsx

**Descripción:**  
El estado de autenticación (token JWT, usuario actual) se centraliza en un contexto React accesible desde cualquier componente mediante el hook `useAuth`.

```jsx
const { token, usuario, login, logout } = useAuth();
```

**Justificación:**  
Sin Context, el token debería pasarse como prop por toda la cadena de componentes (prop drilling), haciendo el código difícil de mantener. Context Provider resuelve este problema centralizando el estado global de autenticación.

---

### 4.2 Patrón Custom Hook

**Componente:** useAuth.js

**Descripción:**  
Encapsula la lógica de lectura del contexto de autenticación, evitando acceso directo a `useContext(AuthContext)` desde múltiples componentes.

**Justificación:**  
Los Custom Hooks son el equivalente React del patrón de abstracción. Permiten reutilizar lógica entre componentes sin duplicar código y hacen los componentes más fáciles de testear.

---

### 4.3 Patrón Higher-Order Component (HOC) — Rutas Protegidas

**Componente:** PrivateRoute en App.jsx

**Descripción:**  
```jsx
function PrivateRoute({ children }) {
  const { token } = useAuth();
  return token ? children : <Navigate to="/login" replace />;
}
```

**Justificación:**  
El HOC PrivateRoute envuelve las páginas que requieren autenticación, separando la lógica de autorización de la lógica de la página. Si cambia el mecanismo de autenticación, solo se modifica este componente.

---

## 5. Resumen de patrones por componente

| Componente | Patrones aplicados |
|---|---|
| bff-web | BFF/Proxy |
| ms-auth | Repository, Builder, DTO |
| ms-mascotas | Repository, Builder, Publisher, Circuit Breaker |
| ms-geolocalizacion | Repository, Builder, DTO |
| ms-matching | Repository, Builder, Consumer, Strategy |
| frontend | Context/Provider, Custom Hook, HOC (PrivateRoute) |

---

## 6. Conclusiones

Los patrones seleccionados responden directamente a los problemas del dominio de Sanos y Salvos:

- **Repository** garantiza que cada microservicio pueda evolucionar su capa de datos de forma independiente.
- **Event-Driven + Circuit Breaker** permiten que el motor de coincidencias opere sin degradar el registro de mascotas.
- **Strategy** hace que el algoritmo de matching sea extensible sin modificar el orquestador.
- **BFF** protege la arquitectura interna y simplifica el cliente web.
- **Context + Custom Hook** en React centralizan el estado de autenticación y evitan prop drilling.

La combinación de estos patrones produce un sistema mantenible, escalable y resiliente frente a fallos parciales.
