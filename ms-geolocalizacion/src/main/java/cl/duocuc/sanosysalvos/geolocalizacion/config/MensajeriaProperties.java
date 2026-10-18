package cl.duocuc.sanosysalvos.geolocalizacion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Nombres de exchanges, colas y routing keys de ms-geolocalizacion,
 * centralizados en application.properties (prefijo {@code mensajeria}).
 */
@Data
@ConfigurationProperties(prefix = "mensajeria")
public class MensajeriaProperties {

    /** Exchange topic compartido por todos los microservicios. */
    private String exchange;

    /** Exchange direct de dead-letter. */
    private String dlx;

    /** Cola de entrada de este microservicio, su DLQ y las routing keys que escucha. */
    private Cola cola = new Cola();

    /** Routing keys de los eventos que publica este microservicio. */
    private RoutingKeys routingKeys = new RoutingKeys();

    /** Cola donde terminan los mensajes publicados que el broker no pudo enrutar. */
    private NoEntregados noEntregados = new NoEntregados();

    @Data
    public static class Cola {
        private String nombre;
        private String dlq;
        private List<String> bindingKeys = new ArrayList<>();
    }

    @Data
    public static class RoutingKeys {
        private String zonaActualizada;
    }

    @Data
    public static class NoEntregados {
        private String cola;
        private String routingKey;
    }
}
