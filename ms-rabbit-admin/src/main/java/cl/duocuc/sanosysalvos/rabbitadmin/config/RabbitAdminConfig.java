package cl.duocuc.sanosysalvos.rabbitadmin.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuración de infraestructura del administrador.
 * <ul>
 *   <li>{@code AmqpAdmin} (RabbitAdmin) lo autoconfigura Spring Boot y se usa
 *       para declarar/eliminar colas, exchanges y bindings vía AMQP.</li>
 *   <li>{@link RabbitTemplate} con conversor JSON para publicar mensajes de prueba.</li>
 *   <li>{@link RestClient} hacia la API HTTP de management (solo lecturas).</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(RabbitAdminProperties.class)
public class RabbitAdminConfig {

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public RestClient managementRestClient(RabbitAdminProperties properties) {
        RabbitAdminProperties.Management m = properties.getManagement();
        return RestClient.builder()
                .defaultHeaders(h -> h.setBasicAuth(m.getUsername(), m.getPassword()))
                .build();
    }
}
