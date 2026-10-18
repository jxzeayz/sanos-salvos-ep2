package cl.duocuc.sanosysalvos.mascotas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges y routing keys de ms-mascotas, centralizados en
 * application.properties (prefijo {@code mensajeria}).
 */
@Data
@ConfigurationProperties(prefix = "mensajeria")
public class MensajeriaProperties {

    /** Exchange topic compartido por todos los microservicios. */
    private String exchange;

    /** Exchange direct de dead-letter. */
    private String dlx;

    /** Routing keys de los eventos que publica este microservicio. */
    private RoutingKeys routingKeys = new RoutingKeys();

    /** Cola donde terminan los mensajes publicados que el broker no pudo enrutar. */
    private NoEntregados noEntregados = new NoEntregados();

    @Data
    public static class RoutingKeys {
        /** Prefijo de los eventos por estado: {prefijo}.perdida, {prefijo}.encontrada, {prefijo}.reunificada. */
        private String prefijoMascota;
        /** Routing key cuando se elimina una mascota. */
        private String mascotaEliminada;
    }

    @Data
    public static class NoEntregados {
        private String cola;
        private String routingKey;
    }
}
