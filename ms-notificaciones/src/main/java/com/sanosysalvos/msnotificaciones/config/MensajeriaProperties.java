package com.sanosysalvos.msnotificaciones.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys de ms-notificaciones, centralizados
 * en application.properties (prefijo {@code mensajeria}).
 */
@Data
@ConfigurationProperties(prefix = "mensajeria")
public class MensajeriaProperties {

    /** Exchange topic compartido por todos los microservicios. */
    private String exchange;

    /** Exchange direct de dead-letter. */
    private String dlx;

    /** Cola de entrada de este microservicio y su DLQ. */
    private Cola cola = new Cola();

    @Data
    public static class Cola {
        private String nombre;
        private String dlq;
        private String bindingKey;
    }
}
