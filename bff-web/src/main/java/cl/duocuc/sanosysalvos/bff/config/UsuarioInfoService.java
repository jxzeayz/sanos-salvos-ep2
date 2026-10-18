package cl.duocuc.sanosysalvos.bff.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resuelve el perfil del usuario autenticado (usuarioId, rol, etc.) a partir
 * del email contenido en el access token de Auth0, consultando ms-auth.
 *
 * Con Auth0 el token ya no contiene el claim "usuarioId" (el ID interno en BD),
 * por lo que se realiza este mapeo email → perfil con una caché corta para
 * no consultar ms-auth en cada petición.
 */
@Component
@RequiredArgsConstructor
public class UsuarioInfoService {

    private static final long CACHE_TTL_MS = 60_000;

    @Qualifier("authClient")
    private final WebClient authClient;

    private final ConcurrentHashMap<String, Entrada> cache = new ConcurrentHashMap<>();

    public Long getUsuarioId(Claims claims, String authHeader) {
        Object id = perfil(claims, authHeader).get("usuarioId");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return id != null ? Long.valueOf(id.toString()) : null;
    }

    public Map<String, Object> perfil(Claims claims, String authHeader) {
        String email = email(claims);
        long now = System.currentTimeMillis();

        Entrada entrada = cache.get(email);
        if (entrada != null && now - entrada.timestamp < CACHE_TTL_MS) {
            return entrada.datos;
        }

        Map<String, Object> datos = authClient.get()
                .uri("/api/auth/perfil")
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (datos == null) {
            datos = Map.of();
        }
        cache.put(email, new Entrada(now, datos));
        return datos;
    }

    private String email(Claims claims) {
        Object email = claims.get("email");
        return email != null ? email.toString() : claims.getSubject();
    }

    private record Entrada(long timestamp, Map<String, Object> datos) {
    }
}