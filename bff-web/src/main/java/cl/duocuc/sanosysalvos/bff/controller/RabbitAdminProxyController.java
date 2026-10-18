package cl.duocuc.sanosysalvos.bff.controller;

import cl.duocuc.sanosysalvos.bff.config.Errores;
import cl.duocuc.sanosysalvos.bff.config.JwtValidator;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Proxy del BFF hacia ms-rabbit-admin: /bff/rabbit/** → /api/rabbit/**.
 * Conserva método, query params, body y token. Solo usuarios con rol ADMIN.
 */
@RestController
@RequestMapping("/bff/rabbit")
public class RabbitAdminProxyController {

    private static final String CLAIM_ROL = "https://sanosysalvos/rol";
    private static final MultiValueMap<String, String> SIN_PARAMS = new LinkedMultiValueMap<>();

    private final WebClient rabbitAdminClient;
    private final JwtValidator jwtValidator;
    private final String rabbitAdminUrl;

    public RabbitAdminProxyController(@Qualifier("rabbitAdminClient") WebClient rabbitAdminClient,
                                      JwtValidator jwtValidator,
                                      @Value("${services.rabbit-admin.url}") String rabbitAdminUrl) {
        this.rabbitAdminClient = rabbitAdminClient;
        this.jwtValidator = jwtValidator;
        this.rabbitAdminUrl = rabbitAdminUrl;
    }

    // ─── Colas ──────────────────────────────────────────────────────────────

    @GetMapping("/queues")
    public Mono<ResponseEntity<String>> listarColas(@RequestHeader(value = "Authorization", required = false) String auth) {
        return reenviar(HttpMethod.GET, auth, null, SIN_PARAMS, "/api/rabbit/queues");
    }

    @GetMapping("/queues/{nombre}")
    public Mono<ResponseEntity<String>> obtenerCola(@RequestHeader(value = "Authorization", required = false) String auth,
                                                    @PathVariable String nombre) {
        return reenviar(HttpMethod.GET, auth, null, SIN_PARAMS, "/api/rabbit/queues/{nombre}", nombre);
    }

    @PostMapping("/queues")
    public Mono<ResponseEntity<String>> crearCola(@RequestHeader(value = "Authorization", required = false) String auth,
                                                  @RequestBody(required = false) String body) {
        return reenviar(HttpMethod.POST, auth, body, SIN_PARAMS, "/api/rabbit/queues");
    }

    @DeleteMapping("/queues/{nombre}")
    public Mono<ResponseEntity<String>> eliminarCola(@RequestHeader(value = "Authorization", required = false) String auth,
                                                     @PathVariable String nombre,
                                                     @RequestParam MultiValueMap<String, String> params) {
        return reenviar(HttpMethod.DELETE, auth, null, params, "/api/rabbit/queues/{nombre}", nombre);
    }

    @GetMapping("/queues/{nombre}/messages")
    public Mono<ResponseEntity<String>> leerMensajes(@RequestHeader(value = "Authorization", required = false) String auth,
                                                     @PathVariable String nombre,
                                                     @RequestParam MultiValueMap<String, String> params) {
        return reenviar(HttpMethod.GET, auth, null, params, "/api/rabbit/queues/{nombre}/messages", nombre);
    }

    @DeleteMapping("/queues/{nombre}/messages")
    public Mono<ResponseEntity<String>> purgarCola(@RequestHeader(value = "Authorization", required = false) String auth,
                                                   @PathVariable String nombre) {
        return reenviar(HttpMethod.DELETE, auth, null, SIN_PARAMS, "/api/rabbit/queues/{nombre}/messages", nombre);
    }

    // ─── Exchanges ──────────────────────────────────────────────────────────

    @GetMapping("/exchanges")
    public Mono<ResponseEntity<String>> listarExchanges(@RequestHeader(value = "Authorization", required = false) String auth) {
        return reenviar(HttpMethod.GET, auth, null, SIN_PARAMS, "/api/rabbit/exchanges");
    }

    @PostMapping("/exchanges")
    public Mono<ResponseEntity<String>> crearExchange(@RequestHeader(value = "Authorization", required = false) String auth,
                                                      @RequestBody(required = false) String body) {
        return reenviar(HttpMethod.POST, auth, body, SIN_PARAMS, "/api/rabbit/exchanges");
    }

    @DeleteMapping("/exchanges/{nombre}")
    public Mono<ResponseEntity<String>> eliminarExchange(@RequestHeader(value = "Authorization", required = false) String auth,
                                                         @PathVariable String nombre) {
        return reenviar(HttpMethod.DELETE, auth, null, SIN_PARAMS, "/api/rabbit/exchanges/{nombre}", nombre);
    }

    // ─── Bindings y mensajes ────────────────────────────────────────────────

    @GetMapping("/bindings")
    public Mono<ResponseEntity<String>> listarBindings(@RequestHeader(value = "Authorization", required = false) String auth) {
        return reenviar(HttpMethod.GET, auth, null, SIN_PARAMS, "/api/rabbit/bindings");
    }

    @PostMapping("/bindings")
    public Mono<ResponseEntity<String>> crearBinding(@RequestHeader(value = "Authorization", required = false) String auth,
                                                     @RequestBody(required = false) String body) {
        return reenviar(HttpMethod.POST, auth, body, SIN_PARAMS, "/api/rabbit/bindings");
    }

    @DeleteMapping("/bindings")
    public Mono<ResponseEntity<String>> eliminarBinding(@RequestHeader(value = "Authorization", required = false) String auth,
                                                        @RequestParam MultiValueMap<String, String> params) {
        return reenviar(HttpMethod.DELETE, auth, null, params, "/api/rabbit/bindings");
    }

    @PostMapping("/messages")
    public Mono<ResponseEntity<String>> publicarMensaje(@RequestHeader(value = "Authorization", required = false) String auth,
                                                        @RequestBody(required = false) String body) {
        return reenviar(HttpMethod.POST, auth, body, SIN_PARAMS, "/api/rabbit/messages");
    }

    // ─── Reenvío común ──────────────────────────────────────────────────────

    private Mono<ResponseEntity<String>> reenviar(HttpMethod metodo, String auth, String body,
                                                  MultiValueMap<String, String> params,
                                                  String ruta, Object... variables) {
        Claims claims = validarToken(auth);
        if (claims == null) {
            return Errores.noAutorizado();
        }
        if (!"ADMIN".equals(rol(claims))) {
            return Errores.prohibido("Acceso denegado: solo ADMIN puede administrar RabbitMQ");
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(rabbitAdminUrl)
                .path(ruta)
                .queryParams(params)
                .encode()
                .buildAndExpand(variables)
                .toUri();

        WebClient.RequestBodySpec spec = rabbitAdminClient.method(metodo).uri(uri).header("Authorization", auth);
        WebClient.RequestHeadersSpec<?> peticion = (body != null && !body.isBlank())
                ? spec.contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                : spec;

        return peticion.exchangeToMono(response -> response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(cuerpo -> ResponseEntity.status(response.statusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo)));
    }

    private Claims validarToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
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
