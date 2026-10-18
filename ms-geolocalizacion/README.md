# MS-Geolocalizacion — Microservicio de Geolocalización

**Puerto:** 8082
**Tecnología:** Spring Boot 3.2.5 + JPA + PostGIS (PostgreSQL 16 + postgis 3.4)

## Descripción

Microservicio especializado en el almacenamiento y consulta de reportes geoespaciales. Utiliza **PostGIS** para representar ubicaciones como puntos geográficos (SRID 4326) y ejecutar consultas de proximidad con `ST_DWithin`. El frontend consume este servicio para mostrar el mapa de reportes con Leaflet.

## Patrones aplicados

- **Repository**: `ZonaReporteRepository` con consultas nativas PostGIS
- **Builder**: construcción de entidades `ZonaReporte` con Lombok `@Builder`
- **DTO**: `ZonaReporteRequest` para desacoplar la API de la entidad JPA

## Estructura

```
ms-geolocalizacion/
├── src/main/java/cl/duocuc/sanosysalvos/geolocalizacion/
│   ├── MsGeolocalizacionApplication.java
│   ├── controller/
│   │   └── GeoController.java          # REST endpoints
│   ├── dto/
│   │   └── ZonaReporteRequest.java
│   ├── model/
│   │   └── ZonaReporte.java            # Entidad con campo geometry(Point,4326)
│   ├── repository/
│   │   └── ZonaReporteRepository.java  # Consultas ST_DWithin y zonas calientes
│   └── service/
│       └── GeoService.java
├── src/test/java/.../service/
│   └── GeoServiceTest.java
└── pom.xml
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/geo/zonas` | Registrar reporte con coordenadas |
| GET | `/geo/zonas` | Listar todos los reportes |
| GET | `/geo/zonas/radio?lat=&lon=&radio=` | Buscar reportes en radio (metros) |
| GET | `/geo/zonas/calientes` | Top 10 zonas con más reportes |

### Ejemplo de registro

```json
POST /geo/zonas
{
  "mascotaId": 1,
  "usuarioId": 2,
  "latitud": -33.4569,
  "longitud": -70.6483,
  "tipoReporte": "PERDIDA",
  "descripcion": "Vista cerca del parque"
}
```

### Ejemplo de búsqueda por radio

```
GET /geo/zonas/radio?lat=-33.4569&lon=-70.6483&radio=5000
```
Retorna todos los reportes dentro de 5000 metros del punto indicado.

## Requisitos

- Java 17
- Maven 3.8+
- PostgreSQL 16 con extensión PostGIS 3.4 (`postgis/postgis:16-3.4` en Docker)

## Ejecución local

```bash
cd ms-geolocalizacion
mvn spring-boot:run
```

## Variables de entorno (Docker)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-geo:5432/db_geo
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

## Ejecutar pruebas

```bash
mvn test
```
