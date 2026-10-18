package cl.duocuc.sanosysalvos.bff.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Respuestas de error con cuerpo JSON para el BFF. Centraliza los códigos
 * de estado HTTP (401/403) junto a un mensaje legible para el cliente.
 */
public final class Errores {

    private static final Map<String, Object> NO_AUTORIZADO =
            Map.of("mensaje", "Token inválido o expirado");

    private Errores() {
    }

    public static <T> Mono<ResponseEntity<T>> noAutorizado() {
        return mono(HttpStatus.UNAUTHORIZED, NO_AUTORIZADO);
    }

    public static <T> Mono<ResponseEntity<T>> prohibido(String mensaje) {
        return mono(HttpStatus.FORBIDDEN, Map.of("mensaje", mensaje));
    }

    @SuppressWarnings("unchecked")
    private static <T> Mono<ResponseEntity<T>> mono(HttpStatus status, Object body) {
        return Mono.just((ResponseEntity<T>) (ResponseEntity<?>) ResponseEntity.status(status).body(body));
    }
}