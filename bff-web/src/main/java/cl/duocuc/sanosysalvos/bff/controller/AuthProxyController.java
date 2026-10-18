package cl.duocuc.sanosysalvos.bff.controller;

import cl.duocuc.sanosysalvos.bff.config.Errores;
import cl.duocuc.sanosysalvos.bff.config.JwtValidator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/auth")
@RequiredArgsConstructor
public class AuthProxyController {

    @Qualifier("authClient")
    private final WebClient authClient;

    private final JwtValidator jwtValidator;

    @GetMapping("/perfil")
    public Mono<ResponseEntity<Map>> getPerfil(
            @RequestHeader("Authorization") String authHeader) {
        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        return authClient.get()
                .uri("/api/auth/perfil")
                .header("Authorization", authHeader)
                .exchangeToMono(response ->
                        response.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .map(responseBody ->
                                        ResponseEntity
                                                .status(response.statusCode())
                                                .body(responseBody)
                                )
                );
    }

    @PutMapping("/perfil")
    public Mono<ResponseEntity<Map>> updatePerfil(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        return authClient.put()
                .uri("/api/auth/perfil")
                .header("Authorization", authHeader)
                .bodyValue(body)
                .exchangeToMono(response ->
                        response.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .map(responseBody ->
                                        ResponseEntity
                                                .status(response.statusCode())
                                                .body(responseBody)
                                )
                );
    }

    @PutMapping("/password")
    public Mono<ResponseEntity<Map>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        return authClient.put()
                .uri("/api/auth/password")
                .header("Authorization", authHeader)
                .bodyValue(body)
                .exchangeToMono(response ->
                        response.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .map(responseBody ->
                                        ResponseEntity
                                                .status(response.statusCode())
                                                .body(responseBody)
                                )
                );
    }

    @GetMapping("/usuarios")
    public Mono<ResponseEntity<List>> listarUsuarios(
            @RequestHeader("Authorization") String authHeader) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }
        String rol = rol(claims);
        if (!"ADMIN".equals(rol)) {
            return Errores.prohibido("Acceso denegado: solo ADMIN");
        }

        return authClient.get()
                .uri("/api/auth/usuarios")
                .header("Authorization", authHeader)
                .exchangeToMono(response ->
                        response.bodyToMono(List.class)
                                .defaultIfEmpty(List.of())
                                .map(responseBody ->
                                        ResponseEntity
                                                .status(response.statusCode())
                                                .body(responseBody)
                                )
                );
    }

    @DeleteMapping("/usuarios/{id}")
    public Mono<ResponseEntity<Map>> eliminarUsuario(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }
        String rol = rol(claims);
        if (!"ADMIN".equals(rol)) {
            return Errores.prohibido("Acceso denegado: solo ADMIN");
        }

        return authClient.delete()
                .uri("/api/auth/usuarios/{id}", id)
                .header("Authorization", authHeader)
                .exchangeToMono(response ->
                        response.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .map(responseBody ->
                                        ResponseEntity
                                                .status(response.statusCode())
                                                .body(responseBody)
                                )
                );
    }

    private Claims validarToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            return jwtValidator.validate(authHeader.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    private static final String CLAIM_ROL = "https://sanosysalvos/rol";

    @PostMapping("/sync")
    public Mono<ResponseEntity<Map>> sincronizar(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, Object> body) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        Map<String, Object> payload = body == null ? new java.util.HashMap<>() : new java.util.HashMap<>(body);
        payload.put("rol", rol(claims));
        Object nombre = claims.get("nombre");
        if (nombre != null) {
            payload.put("nombre", nombre);
        }
        Object email = claims.get("email");
        if (email != null) {
            payload.put("email", email);
        }

        return authClient.post()
                .uri("/api/auth/sync")
                .header("Authorization", authHeader)
                .bodyValue(payload)
                .exchangeToMono(response ->
                        response.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .map(responseBody -> {
                                    Map<String, Object> perfil = new java.util.HashMap<>(responseBody);
                                    // Echo del access token de Auth0 para la sesión local del frontend
                                    perfil.put("token", authHeader.substring(7));
                                    return ResponseEntity
                                            .status(response.statusCode())
                                            .body(perfil);
                                })
                );
    }

    private String rol(Claims claims) {
        Object valor = claims.get(CLAIM_ROL);
        if (valor != null) {
            return valor.toString();
        }
        return claims.get("rol", String.class);
    }
}