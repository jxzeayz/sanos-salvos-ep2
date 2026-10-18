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
@RequestMapping("/bff/contacto")
@RequiredArgsConstructor
public class ContactoProxyController {

    @Qualifier("matchingClient")
    private final WebClient matchingClient;

    @Qualifier("mascotasClient")
    private final WebClient mascotasClient;

    private final JwtValidator jwtValidator;

    private final UsuarioInfoService usuarioInfoService;

    @GetMapping("/mascota/{mascotaId}")
    public Mono<ResponseEntity<Map>> obtenerContacto(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long mascotaId) {

        Claims claims = validarToken(authHeader);
        if (claims == null) {
            return Errores.noAutorizado();
        }

        Long usuarioId = getUsuarioId(claims, authHeader);

        return matchingClient.get()
                .uri("/api/matching/coincidencias/mascota/{mascotaId}", mascotaId)
                .exchangeToMono(response -> response.bodyToMono(List.class)
                        .defaultIfEmpty(List.of())
                        .flatMap(coincidencias -> {
                            boolean tieneConfirmada = false;
                            Long contactoUsuarioId = null;
                            Long contactoMascotaId = null;

                            if (coincidencias instanceof List<?> list) {
                                for (Object obj : list) {
                                    if (obj instanceof Map<?, ?> c) {
                                        Object estado = c.get("estado");
                                        if ("CONFIRMADA".equals(estado)) {
                                            Object mpId = c.get("mascotaPerdidaId");
                                            Object meId = c.get("mascotaEncontradaId");
                                            Long mpLong = mpId instanceof Number ? ((Number) mpId).longValue() : null;
                                            Long meLong = meId instanceof Number ? ((Number) meId).longValue() : null;

                                            if (mascotaId.equals(mpLong)) {
                                                Object uidObj = c.get("usuarioIdEncontrada");
                                                contactoUsuarioId = uidObj instanceof Number ? ((Number) uidObj).longValue() : null;
                                                contactoMascotaId = meLong;
                                            } else if (mascotaId.equals(meLong)) {
                                                Object uidObj = c.get("usuarioIdPerdida");
                                                contactoUsuarioId = uidObj instanceof Number ? ((Number) uidObj).longValue() : null;
                                                contactoMascotaId = mpLong;
                                            }

                                            if (contactoUsuarioId != null && !usuarioId.equals(contactoUsuarioId)) {
                                                tieneConfirmada = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (!tieneConfirmada) {
                                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                       .<Map>body(Map.of("mensaje", "No hay coincidencia confirmada para acceder al contacto")));
                            }

                            Long finalContactoMascotaId = contactoMascotaId;
                            return mascotasClient.get()
                                    .uri("/api/mascotas/{id}", finalContactoMascotaId)
                                    .exchangeToMono(mascotaResp -> mascotaResp.bodyToMono(Map.class)
                                            .defaultIfEmpty(Map.of())
                                            .map(mascota -> {
                                                java.util.Map<String, Object> contacto = new java.util.HashMap<>();
                                                contacto.put("nombre", mascota.get("nombre"));
                                                contacto.put("especie", mascota.get("especie"));
                                                contacto.put("raza", mascota.get("raza"));
                                                contacto.put("color", mascota.get("color"));
                                                contacto.put("descripcion", mascota.get("descripcion"));
                                                return ResponseEntity.ok((Map) contacto);
                                            })
                                    );
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
