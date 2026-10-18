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
@RequestMapping("/bff/auditoria")
@RequiredArgsConstructor
public class AuditoriaProxyController {

    private static final String CLAIM_ROL = "https://sanosysalvos/rol";

    @Qualifier("auditoriaClient")
    private final WebClient auditoriaClient;

    private final JwtValidator jwtValidator;

    @GetMapping("/logs")
    public Mono<ResponseEntity<List>> listar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String servicio,
            @RequestParam(required = false) String evento) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        String rol = rol(claims);
        if (!"ADMIN".equals(rol)) {
            return Errores.prohibido("Acceso denegado: solo ADMIN");
        }

        String uri;
        if (servicio != null) {
            uri = "/api/audit/logs/servicio?servicio=" + servicio;
        } else if (evento != null) {
            uri = "/api/audit/logs/evento?evento=" + evento;
        } else {
            uri = "/api/audit/logs";
        }

        return auditoriaClient.get()
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

    @GetMapping("/log/{id}")
    public Mono<ResponseEntity<Map>> obtener(
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

        return auditoriaClient.get()
                .uri("/api/audit/logs/{id}", id)
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

    @GetMapping("/estadisticas")
    public Mono<ResponseEntity<Map>> estadisticas(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        String rol = rol(claims);
        if (!"ADMIN".equals(rol)) {
            return Errores.prohibido("Acceso denegado: solo ADMIN");
        }

        return auditoriaClient.get()
                .uri("/api/audit/logs")
                .header("Authorization", authHeader)
                .exchangeToMono(response ->
                        response.bodyToMono(List.class)
                                .defaultIfEmpty(List.of())
                                .map(responseBody -> ResponseEntity.ok((Map) Map.of(
                                        "totalEventos", responseBody.size(),
                                        "logs", responseBody
                                )))
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

    private String rol(Claims claims) {
        Object valor = claims.get(CLAIM_ROL);
        if (valor != null) {
            return valor.toString();
        }
        return claims.get("rol", String.class);
    }
}