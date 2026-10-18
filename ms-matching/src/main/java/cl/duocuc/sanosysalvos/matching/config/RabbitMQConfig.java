package cl.duocuc.sanosysalvos.matching.config;

import cl.duocuc.sanosysalvos.matching.event.MensajesNoEntregadosHandler;
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
 * Configuración de mensajería de ms-matching. Toda la topología RabbitMQ del
 * microservicio se declara aquí; los nombres vienen de {@link MensajeriaProperties}.
 *
 * <pre>
 * ENTRADA  ms-mascotas ──[mascota.*]──▶ sanos-salvos.events ──▶ q.matching ──▶ MascotaEventConsumer
 *          rechazo (NACK sin reencolar) ──▶ sanos-salvos.dlx ──[q.matching.dlq]──▶ q.matching.dlq
 *
 * SALIDA   CoincidenciaEventPublisher ──[coincidencia.hallada]──▶ sanos-salvos.events
 *          ──▶ q.notificaciones (ms-notificaciones) y q.auditoria (ms-auditoria)
 *          sin ruta (returned) ──▶ sanos-salvos.dlx ──[no-entregados]──▶ q.no-entregados
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitMQConfig {

    private final MensajeriaProperties props;

    public RabbitMQConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ─── Exchanges ──────────────────────────────────────────────────────────

    /** Exchange de eventos de dominio (compartido). */
    @Bean
    public TopicExchange eventosExchange() {
        return ExchangeBuilder.topicExchange(props.getExchange()).durable(true).build();
    }

    /** Exchange de dead-letter (compartido). */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.getDlx()).durable(true).build();
    }

    // ─── Caso de uso: detectar coincidencias al registrar/actualizar mascotas ─

    /** Cola de entrada; los mensajes rechazados se desvían a su DLQ. */
    @Bean
    public Queue matchingQueue() {
        return QueueBuilder.durable(props.getCola().getNombre())
                .deadLetterExchange(props.getDlx())
                .deadLetterRoutingKey(props.getCola().getDlq())
                .build();
    }

    @Bean
    public Binding matchingBinding() {
        return BindingBuilder.bind(matchingQueue()).to(eventosExchange()).with(props.getCola().getBindingKey());
    }

    /** DLQ de matching: guarda los eventos que no se pudieron procesar. */
    @Bean
    public Queue matchingDlq() {
        return QueueBuilder.durable(props.getCola().getDlq()).build();
    }

    @Bean
    public Binding matchingDlqBinding() {
        return BindingBuilder.bind(matchingDlq()).to(deadLetterExchange()).with(props.getCola().getDlq());
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

    /**
     * Template con publisher confirms y mandatory=true: si el broker no puede
     * enrutar o confirmar un mensaje, {@link MensajesNoEntregadosHandler} lo
     * registra en el log y lo reenvía a la cola de no entregados.
     */
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
