# MS-Archivos — Microservicio de Gestión de Archivos

**Puerto:** 8086
**Tecnología:** Spring Boot 3.2.5 + JPA + MinIO (object storage) + PostgreSQL

## Descripción

Microservicio encargado de la gestión de fotos e imágenes de mascotas. Utiliza **MinIO** para almacenamiento de objetos S3-compatible y genera URLs temporales firmadas (presigned URLs) para acceso seguro a las imágenes. Cada archivo subido se registra en PostgreSQL con metadatos (nombre, MIME type, tamaño, mascota asociada).

## Patrones aplicados

- **Repository**: acceso a datos mediante `ArchivoFotoRepository`
- **Builder**: construcción de `ArchivoFoto` con Lombok `@Builder`

## Flujo de subida

```
Frontend → POST /bff/archivos/subir (multipart)
    └── BFF-Web → ms-archivos
            └── ArchivoFotoService.subirArchivo()
                    ├── MinIO: putObject (bucket "archivos")
                    ├── MinIO: getPresignedObjectUrl (URL temporal 24h)
                    └── PostgreSQL: guardar metadatos
```

## Estructura

```
ms-archivos/
├── src/main/java/com/sanosysalvos/msarchivos/
│   ├── MsArchivosApplication.java
│   ├── config/
│   │   ├── MinioConfig.java               # Cliente MinIO (endpoint, credentials)
│   │   ├── GlobalExceptionHandler.java    # Manejo centralizado de errores
│   │   └── GlobalCorsConfig.java          # CORS
│   ├── controller/
│   │   └── ArchivoController.java         # REST endpoints (CRUD + upload)
│   ├── model/
│   │   └── ArchivoFoto.java               # Entidad JPA
│   ├── repository/
│   │   └── ArchivoFotoRepository.java
│   └── service/
│       └── ArchivoFotoService.java        # Lógica MinIO + PostgreSQL
├── src/test/java/.../service/
│   └── ArchivoFotoServiceTest.java
└── pom.xml
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/archivos?file=&mascotaId=` | Subir archivo (multipart) |
| DELETE | `/api/archivos/{id}` | Eliminar archivo (MinIO + DB) |
| GET | `/api/archivos/{id}/url` | Obtener URL temporal firmada (24h) |
| GET | `/api/archivos/mascota/{mascotaId}` | Listar archivos de una mascota |

### Ejemplo de respuesta (subida)

```json
{
  "id": 1,
  "nombreArchivo": "firulais.jpg",
  "urlPublica": "http://minio:9000/archivos/a1b2c3d4.jpg?X-Amz-...",
  "mimeType": "image/jpeg",
  "tamanioBytes": 245862,
  "mascotaId": 1,
  "subidoEn": "2026-06-18T10:30:00"
}
```

## Requisitos

- Java 17
- Maven 3.8+
- PostgreSQL 16 (base de datos `db_archivos`)
- MinIO (almacenamiento de objetos, puerto 9000/9001)

## Ejecución local

```bash
cd ms-archivos
mvn spring-boot:run
```

## Variables de entorno (Docker)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-archivos:5432/db_archivos
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_MINIO_ENDPOINT=http://minio:9000
SPRING_MINIO_ACCESSKEY=minioadmin
SPRING_MINIO_SECRETKEY=minioadmin
SPRING_MINIO_BUCKET=archivos
```

## Ejecutar pruebas

```bash
mvn test
```
