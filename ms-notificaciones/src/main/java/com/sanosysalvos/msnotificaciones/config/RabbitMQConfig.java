package com.sanosysalvos.msnotificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería de ms-notificaciones. Los nombres vienen de
 * {@link MensajeriaProperties}.
 *
 * <pre>
 * ms-matching ──[coincidencia.*]──▶ sanos-salvos.events ──▶ q.notificaciones ──▶ CoincidenciaEventConsumer
 * rechazo (NACK sin reencolar) ──▶ sanos-salvos.dlx ──[q.notificaciones.dlq]──▶ q.notificaciones.dlq
 * </pre>
 */
@Configuration
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

    // ─── Caso de uso: notificar al dueño cuando se detecta una coincidencia ─

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(props.getCola().getNombre())
                .deadLetterExchange(props.getDlx())
                .deadLetterRoutingKey(props.getCola().getDlq())
                .build();
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(eventosExchange())
                .with(props.getCola().getBindingKey());
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.getCola().getDlq()).build();
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq()).to(deadLetterExchange()).with(props.getCola().getDlq());
    }
}
