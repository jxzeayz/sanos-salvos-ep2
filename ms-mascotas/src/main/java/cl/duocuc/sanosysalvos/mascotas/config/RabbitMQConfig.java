package cl.duocuc.sanosysalvos.mascotas.config;

import cl.duocuc.sanosysalvos.mascotas.event.MensajesNoEntregadosHandler;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería de ms-mascotas (solo productor). Los nombres
 * vienen de {@link MensajeriaProperties}.
 *
 * <pre>
 * MascotaEventPublisher ──[mascota.perdida | mascota.encontrada | mascota.reunificada | mascota.eliminada]──▶
 *     sanos-salvos.events ──▶ q.matching, q.geolocalizacion, q.auditoria
 * sin ruta (returned) ──▶ sanos-salvos.dlx ──[no-entregados]──▶ q.no-entregados
 * </pre>
 * Las colas de destino las declara cada microservicio consumidor.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitMQConfig {

    private final MensajeriaProperties props;

    public RabbitMQConfig(MensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange mascotasExchange() {
        return ExchangeBuilder.topicExchange(props.getExchange()).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.getDlx()).durable(true).build();
    }

    // ─── Caso de uso: mensajes publicados que el broker no pudo enrutar ─────

    @Bean
    public Queue noEntregadosQueue() {
        return QueueBuilder.durable(props.getNoEntregados().getCola()).build();
    }

    @Bean
    public Binding noEntregadosBinding() {
        return BindingBuilder.bind(noEntregadosQueue()).to(deadLetterExchange())
                .with(props.getNoEntregados().getRoutingKey());
    }

    // ─── Infraestructura ────────────────────────────────────────────────────

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        MensajesNoEntregadosHandler handler = new MensajesNoEntregadosHandler(
                template, props.getDlx(), props.getNoEntregados().getRoutingKey());
        template.setReturnsCallback(handler);
        template.setConfirmCallback(handler);
        return template;
    }
}
