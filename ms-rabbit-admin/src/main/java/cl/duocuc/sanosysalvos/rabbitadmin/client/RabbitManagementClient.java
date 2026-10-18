package cl.duocuc.sanosysalvos.rabbitadmin.client;

import cl.duocuc.sanosysalvos.rabbitadmin.config.RabbitAdminProperties;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.BrokerNoDisponibleException;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.RecursoNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API HTTP de management de RabbitMQ.
 * Se usa solo para operaciones que AMQP no ofrece: listar recursos y leer
 * mensajes de una cola sin consumirlos. Las creaciones y eliminaciones se
 * hacen por AMQP en {@code RabbitAdminService}.
 */
@Slf4j
@Component
public class RabbitManagementClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final RabbitAdminProperties.Management config;

    public RabbitManagementClient(RestClient managementRestClient, RabbitAdminProperties properties) {
        this.restClient = managementRestClient;
        this.config = properties.getManagement();
    }

    public List<Map<String, Object>> listarColas() {
        return get("/api/queues/" + vhost());
    }

    public List<Map<String, Object>> listarExchanges() {
        return get("/api/exchanges/" + vhost());
    }

    public List<Map<String, Object>> listarBindings() {
        return get("/api/bindings/" + vhost());
    }

    /**
     * Lee hasta {@code cantidad} mensajes y los devuelve a la cola
     * (ackmode ack_requeue_true), por lo que no se pierden.
     */
    public List<Map<String, Object>> leerMensajesSinConsumir(String cola, int cantidad) {
        URI uri = uri("/api/queues/" + vhost() + "/" + segmento(cola) + "/get");
        Map<String, Object> body = Map.of(
                "count", cantidad,
                "ackmode", "ack_requeue_true",
                "encoding", "auto",
                "truncate", 50_000);
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(LISTA);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecursoNoEncontradoException("La cola '" + cola + "' no existe");
        } catch (RestClientException e) {
            throw noDisponible(e);
        }
    }

    private List<Map<String, Object>> get(String path) {
        try {
            List<Map<String, Object>> resultado = restClient.get().uri(uri(path)).retrieve().body(LISTA);
            return resultado != null ? resultado : List.of();
        } catch (RestClientException e) {
            throw noDisponible(e);
        }
    }

    private BrokerNoDisponibleException noDisponible(RestClientException e) {
        log.error("Error consultando la API de management de RabbitMQ: {}", e.getMessage());
        return new BrokerNoDisponibleException("No se pudo consultar la API de management de RabbitMQ", e);
    }

    private URI uri(String path) {
        String base = config.getUrl().endsWith("/")
                ? config.getUrl().substring(0, config.getUrl().length() - 1)
                : config.getUrl();
        return URI.create(base + path);
    }

    private String vhost() {
        return segmento(config.getVhost());
    }

    private static String segmento(String valor) {
        return UriUtils.encodePathSegment(valor, StandardCharsets.UTF_8);
    }
}
