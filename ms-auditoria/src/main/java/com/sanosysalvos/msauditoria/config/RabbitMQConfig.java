package com.sanosysalvos.msauditoria.config;

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
 * Configuración de mensajería de ms-auditoria. Los nombres vienen de
 * {@link MensajeriaProperties}.
 *
 * <pre>
 * todos los servicios ──[*.*]──▶ sanos-salvos.events ──▶ q.auditoria ──▶ AuditEventConsumer
 * rechazo (NACK sin reencolar) ──▶ sanos-salvos.dlx ──[q.auditoria.dlq]──▶ q.auditoria.dlq
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

    // ─── Caso de uso: registrar en auditoría todos los eventos de dominio ───

    @Bean
    public Queue auditoriaQueue() {
        return QueueBuilder.durable(props.getCola().getNombre())
                .deadLetterExchange(props.getDlx())
                .deadLetterRoutingKey(props.getCola().getDlq())
                .build();
    }

    @Bean
    public Binding auditoriaBinding() {
        return BindingBuilder.bind(auditoriaQueue()).to(eventosExchange()).with(props.getCola().getBindingKey());
    }

    @Bean
    public Queue auditoriaDlq() {
        return QueueBuilder.durable(props.getCola().getDlq()).build();
    }

    @Bean
    public Binding auditoriaDlqBinding() {
        return BindingBuilder.bind(auditoriaDlq()).to(deadLetterExchange()).with(props.getCola().getDlq());
    }
}
