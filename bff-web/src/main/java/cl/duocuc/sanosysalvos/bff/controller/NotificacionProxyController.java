package cl.duocuc.sanosysalvos.bff.controller;

import cl.duocuc.sanosysalvos.bff.config.Errores;
import cl.duocuc.sanosysalvos.bff.config.JwtValidator;
import cl.duocuc.sanosysalvos.bff.config.UsuarioInfoService;
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
@RequestMapping("/bff/notificaciones")
@RequiredArgsConstructor
public class NotificacionProxyController {

    @Qualifier("notificacionesClient")
    private final WebClient notificacionesClient;

    private final JwtValidator jwtValidator;

    private final UsuarioInfoService usuarioInfoService;

    @GetMapping
    public Mono<ResponseEntity<List>> listar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) boolean soloNoLeidas) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        Long usuarioId = getUsuarioId(claims, authHeader);
        String uri = soloNoLeidas
                ? "/api/notificaciones?usuarioId=" + usuarioId + "&soloNoLeidas=true"
                : "/api/notificaciones?usuarioId=" + usuarioId;

        return notificacionesClient.get()
                .uri(uri)
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

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map>> obtener(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        return notificacionesClient.get()
                .uri("/api/notificaciones/{id}", id)
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

    @PatchMapping("/{id}/leer")
    public Mono<ResponseEntity<Map>> marcarComoLeida(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        return notificacionesClient.patch()
                .uri("/api/notificaciones/{id}/leer", id)
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

    @PatchMapping("/leer-todas")
    public Mono<ResponseEntity<Map>> marcarTodasComoLeidas(
            @RequestHeader("Authorization") String authHeader) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        Long usuarioId = getUsuarioId(claims, authHeader);

        return notificacionesClient.patch()
                .uri("/api/notificaciones/leer-todas?usuarioId=" + usuarioId)
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

    @PostMapping
    public Mono<ResponseEntity<Map>> crearNotificacion(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        body.put("origenUsuarioId", getUsuarioId(claims, authHeader));

        return notificacionesClient.post()
                .uri("/api/notificaciones")
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

    private Long getUsuarioId(Claims claims, String authHeader) {
        return usuarioInfoService.getUsuarioId(claims, authHeader);
    }

    private Claims validarToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            return jwtValidator.validate(authHeader.substring(7));
        } catch (Exception e) {
            return null;
        }
    }
}