# Frontend Sanos y Salvos (Angular 17)

Frontend de la plataforma **Sanos y Salvos**, migrado desde React a **Angular 17** (standalone components) con **Angular Material** y **Leaflet** para el mapa.

## Tecnologías

- Angular 17 (standalone, lazy loading)
- Angular Material (UI)
- Leaflet (mapa de reportes)
- HttpClient + interceptor JWT con refresco automático

## Estructura

```
src/app/
├── core/
│   ├── models/              # Interfaces (usuario, mascota, notificacion, coincidencia)
│   ├── constants/           # Catálogos (estados, especies, roles...)
│   ├── services/            # bff, auth, notificaciones, mascotas, archivos, snackbar
│   ├── guards/              # auth, admin, public
│   ├── interceptors/        # auth.interceptor (Bearer + refresh en 401)
│   └── utils/               # extraerMensajeError
├── components/              # navbar, mascota-card, mascota-form, coincidencia-list, mapa-reportes
└── pages/                   # home, login, register, mascotas, nueva-mascota, mapa, coincidencias, notificaciones, perfil, admin
```

## Desarrollo

```bash
npm install
npm start        # http://localhost:4200 (proxy /bff → http://localhost:8080)
```

## Producción

```bash
npm run build    # dist/sanos-frontend/browser
```

El despliegue se realiza con nginx (proxy `/bff` hacia `bff-web:8080`). Ver `Dockerfile` y `nginx.conf`.