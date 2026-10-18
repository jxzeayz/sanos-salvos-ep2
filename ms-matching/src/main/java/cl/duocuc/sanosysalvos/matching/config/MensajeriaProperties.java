package cl.duocuc.sanosysalvos.matching.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys de ms-matching, centralizados en
 * application.properties (prefijo {@code mensajeria}). Ninguna otra clase
 * escribe estos nombres a mano.
 */
@Data
@ConfigurationProperties(prefix = "mensajeria")
public class MensajeriaProperties {

    /** Exchange topic compartido por todos los microservicios. */
    private String exchange;

    /** Exchange direct de dead-letter (destino de mensajes rechazados). */
    private String dlx;

    /** Cola de entrada de este microservicio y su DLQ. */
    private Cola cola = new Cola();

    /** Routing keys de los eventos que publica este microservicio. */
    private RoutingKeys routingKeys = new RoutingKeys();

    /** Cola donde terminan los mensajes publicados que el broker no pudo enrutar. */
    private NoEntregados noEntregados = new NoEntregados();

    @Data
    public static class Cola {
        private String nombre;
        private String dlq;
        private String bindingKey;
    }

    @Data
    public static class RoutingKeys {
        /** Evento que publica este microservicio. */
        private String coincidenciaHallada;
        /** Evento de entrada que no se procesa (la mascota ya no existe). */
        private String mascotaEliminada;
    }

    @Data
    public static class NoEntregados {
        private String cola;
        private String routingKey;
    }
}
