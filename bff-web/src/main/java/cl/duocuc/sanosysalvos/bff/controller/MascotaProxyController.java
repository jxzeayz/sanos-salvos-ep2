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
@RequestMapping("/bff/mascotas")
@RequiredArgsConstructor
public class MascotaProxyController {

    @Qualifier("mascotasClient")
    private final WebClient mascotasClient;

    @Qualifier("geoClient")
    private final WebClient geoClient;

    @Qualifier("matchingClient")
    private final WebClient matchingClient;

    private final JwtValidator jwtValidator;

    private final UsuarioInfoService usuarioInfoService;

    @PostMapping
    public Mono<ResponseEntity<Map>> registrar(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        body.put("usuarioId", getUsuarioId(claims, authHeader));

        return mascotasClient.post()
                .uri("/api/mascotas")
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

    @GetMapping
    public Mono<ResponseEntity<List>> listar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String estado) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        String uri = estado != null ? "/api/mascotas?estado=" + estado : "/api/mascotas";

        return mascotasClient.get()
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

    @GetMapping("/mis-mascotas")
    public Mono<ResponseEntity<List>> listarMisMascotas(
            @RequestHeader("Authorization") String authHeader) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        Long usuarioId = getUsuarioId(claims, authHeader);

        return mascotasClient.get()
                .uri("/api/mascotas/usuario/{usuarioId}", usuarioId)
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

        return mascotasClient.get()
                .uri("/api/mascotas/{id}", id)
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

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Map>> actualizar(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        body.put("usuarioId", getUsuarioId(claims, authHeader));

        return mascotasClient.put()
                .uri("/api/mascotas/{id}", id)
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

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> eliminar(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        Long usuarioId = getUsuarioId(claims, authHeader);

        return mascotasClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/mascotas/{id}")
                        .queryParam("usuarioId", usuarioId)
                        .build(id))
                .header("Authorization", authHeader)
                .exchangeToMono(response ->
                        Mono.just(ResponseEntity.status(response.statusCode()).build())
                );
    }

    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<Map>> actualizarEstado(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestParam String estado) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        Long usuarioId = getUsuarioId(claims, authHeader);

        return mascotasClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/mascotas/{id}/estado")
                        .queryParam("estado", estado)
                        .queryParam("usuarioId", usuarioId)
                        .build(id))
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

    @GetMapping("/{id}/coincidencias")
    public Mono<ResponseEntity<List>> coincidencias(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        return matchingClient.get()
                .uri("/api/matching/coincidencias/mascota/{id}", id)
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

    @GetMapping("/mapa")
    public Mono<ResponseEntity<List>> mapa(
            @RequestHeader("Authorization") String authHeader) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        return geoClient.get()
                .uri("/api/geo/reportes")
                .header("Authorization", authHeader)
                .exchangeToMono(geoResponse -> geoResponse.bodyToMono(List.class)
                        .defaultIfEmpty(List.of())
                        .flatMap(reportes -> {
                            if (reportes == null || reportes.isEmpty()) {
                                return Mono.just(ResponseEntity.ok((List) List.of()));
                            }

                            java.util.Set<Long> mascotaIds = new java.util.LinkedHashSet<>();
                            for (Object obj : reportes) {
                                if (obj instanceof java.util.Map<?, ?> r) {
                                    Object mid = r.get("mascotaId");
                                    if (mid instanceof Number) {
                                        mascotaIds.add(((Number) mid).longValue());
                                    }
                                }
                            }

                            if (mascotaIds.isEmpty()) {
                                return Mono.just(ResponseEntity.ok((List) reportes));
                            }

                            java.util.List<Mono<Map>> monos = new java.util.ArrayList<>();
                            java.util.List<Long> idsOrden = new java.util.ArrayList<>(mascotaIds);
                            for (Long mid : idsOrden) {
                                monos.add(mascotasClient.get()
                                        .uri("/api/mascotas/{id}", mid)
                                        .header("Authorization", authHeader)
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .defaultIfEmpty(Map.of()));
                            }

                            return reactor.core.publisher.Flux.merge(monos)
                                    .collectList()
                                    .map(mascotasList -> {
                                        java.util.Map<Long, Map> mascotaMap = new java.util.HashMap<>();
                                        for (int i = 0; i < idsOrden.size(); i++) {
                                            mascotaMap.put(idsOrden.get(i), mascotasList.get(i));
                                        }

                                        java.util.List<java.util.Map<String, Object>> enriquecidos = new java.util.ArrayList<>();
                                        for (Object obj : reportes) {
                                            if (obj instanceof java.util.Map<?, ?> geo) {
                                                @SuppressWarnings("unchecked")
                                                java.util.Map<String, Object> enriched = new java.util.HashMap<>((java.util.Map<String, Object>) geo);
                                                Object midObj = geo.get("mascotaId");
                                                if (midObj instanceof Number) {
                                                    Map mascota = mascotaMap.get(((Number) midObj).longValue());
                                                    if (mascota != null) {
                                                        enriched.put("nombre", mascota.get("nombre"));
                                                        enriched.put("especie", mascota.get("especie"));
                                                        enriched.put("raza", mascota.get("raza"));
                                                        enriched.put("color", mascota.get("color"));
                                                        enriched.put("tamano", mascota.get("tamano"));
                                                        enriched.put("descripcion", mascota.get("descripcion") != null ? mascota.get("descripcion") : geo.get("descripcion"));
                                                    }
                                                }
                                                enriquecidos.add(enriched);
                                            }
                                        }
                                        return ResponseEntity.ok((List) enriquecidos);
                                    });
                        })
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