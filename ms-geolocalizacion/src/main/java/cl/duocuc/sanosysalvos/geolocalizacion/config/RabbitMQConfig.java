package cl.duocuc.sanosysalvos.geolocalizacion.config;

import cl.duocuc.sanosysalvos.geolocalizacion.event.MensajesNoEntregadosHandler;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería de ms-geolocalizacion. Los nombres vienen de
 * {@link MensajeriaProperties}.
 *
 * <pre>
 * ENTRADA  ms-mascotas ──[mascota.perdida | mascota.encontrada]──▶ sanos-salvos.events
 *          ──▶ q.geolocalizacion ──▶ MascotaEventConsumer
 *          rechazo (NACK sin reencolar) ──▶ sanos-salvos.dlx ──[q.geolocalizacion.dlq]──▶ q.geolocalizacion.dlq
 *
 * SALIDA   ZonaEventPublisher ──[zona.actualizada]──▶ sanos-salvos.events ──▶ q.auditoria
 *          sin ruta (returned) ──▶ sanos-salvos.dlx ──[no-entregados]──▶ q.no-entregados
 * </pre>
 */
@Configuration
@EnableRabbit
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitMQConfig {

    private final MensajeriaProperties props;

    public RabbitMQConfig(MensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange eventosExchange() {
        return ExchangeBuilder.topicExchange(props.getExchange()).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.getDlx()).durable(true).build();
    }

    // ─── Caso de uso: registrar la zona de cada reporte de mascota ──────────

    @Bean
    public Queue geoQueue() {
        return QueueBuilder.durable(props.getCola().getNombre())
                .deadLetterExchange(props.getDlx())
                .deadLetterRoutingKey(props.getCola().getDlq())
                .build();
    }

    /** Un binding por cada routing key configurada (mascota.perdida, mascota.encontrada). */
    @Bean
    public Declarables geoBindings() {
        return new Declarables(props.getCola().getBindingKeys().stream()
                .map(key -> (Binding) BindingBuilder.bind(geoQueue()).to(eventosExchange()).with(key))
                .toList());
    }

    @Bean
    public Queue geoDlq() {
        return QueueBuilder.durable(props.getCola().getDlq()).build();
    }

    @Bean
    public Binding geoDlqBinding() {
        return BindingBuilder.bind(geoDlq()).to(deadLetterExchange()).with(props.getCola().getDlq());
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
