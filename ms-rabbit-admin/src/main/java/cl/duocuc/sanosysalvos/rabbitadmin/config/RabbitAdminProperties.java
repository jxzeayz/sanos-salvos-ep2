package cl.duocuc.sanosysalvos.rabbitadmin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración centralizada del administrador (prefijo {@code rabbit-admin}
 * en application.properties).
 */
@Data
@ConfigurationProperties(prefix = "rabbit-admin")
public class RabbitAdminProperties {

    /** Colas y exchanges del sistema que la API no permite eliminar. */
    private List<String> recursosProtegidos = new ArrayList<>();

    /** Datos de acceso a la API HTTP de management de RabbitMQ. */
    private Management management = new Management();

    @Data
    public static class Management {
        private String url = "http://localhost:15672";
        private String username = "guest";
        private String password = "guest";
        private String vhost = "/";
    }
}
