package cl.duocuc.sanosysalvos.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig implements WebFilter {

    @Value("${bff.rate-limit.max-requests-per-minute:100}")
    private int maxRequestsPerMinute;

    @Value("${bff.rate-limit.window-ms:60000}")
    private long windowMs;

    private final Map<String, RequestWindow> requestMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);

        RequestWindow window = requestMap.computeIfAbsent(clientIp, k -> new RequestWindow());
        synchronized (window) {
            long now = Instant.now().toEpochMilli();
            if (now - window.windowStart > windowMs) {
                window.count = 0;
                window.windowStart = now;
            }
            if (window.count >= maxRequestsPerMinute) {
                return buildRateLimitResponse(exchange, clientIp);
            }
            window.count++;
        }

        return chain.filter(exchange);
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : "unknown";
    }

    private Mono<Void> buildRateLimitResponse(ServerWebExchange exchange, String clientIp) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", "60");

        String body = "{\"mensaje\":\"Límite de solicitudes excedido. Máximo " + maxRequestsPerMinute
                + " solicitudes por minuto.\",\"segundosParaReintentar\":60}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static class RequestWindow {
        long windowStart = Instant.now().toEpochMilli();
        int count = 0;
    }
}