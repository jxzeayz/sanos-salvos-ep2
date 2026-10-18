# Stage 1: Build Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: nginx + certs + static files
FROM nginx:alpine

# Generar certificados self-signed para desarrollo
RUN apk add --no-cache openssl && \
    mkdir -p /etc/nginx/certs && \
    openssl req -x509 -nodes -days 365 \
      -newkey rsa:2048 \
      -keyout /etc/nginx/certs/localhost.key \
      -out /etc/nginx/certs/localhost.crt \
      -subj "/C=CL/ST=Santiago/L=Santiago/O=Sanos y Salvos/CN=localhost" \
      -addext "subjectAltName=DNS:localhost,DNS:bff-web,IP:127.0.0.1" && \
    apk del openssl

# Copiar archivos estáticos del build de Angular
COPY --from=build /app/dist/sanos-frontend/browser /usr/share/nginx/html

# Copiar configuración nginx
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]