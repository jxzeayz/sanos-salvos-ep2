package cl.duocuc.sanosysalvos.mascotas.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Maneja los mensajes publicados que NO llegaron a ninguna cola:
 * <ul>
 *   <li><b>Returned</b> (sin ruta, mandatory=true): se registran en el log y se
 *       reenvían al exchange de dead-letter → cola de no entregados.</li>
 *   <li><b>Confirm NACK</b> (el broker no aceptó el mensaje): se registra en el log.</li>
 * </ul>
 * El reenvío se hace en un hilo propio porque no se deben usar operaciones del
 * template dentro del hilo del callback.
 */
@Slf4j
public class MensajesNoEntregadosHandler implements RabbitTemplate.ReturnsCallback, RabbitTemplate.ConfirmCallback {

    static final String HEADER_NO_ENTREGADO = "x-no-entregado-desde";

    private final RabbitTemplate template;
    private final String deadLetterExchange;
    private final String routingKeyNoEntregados;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rabbit-no-entregados");
        t.setDaemon(true);
        return t;
    });

    public MensajesNoEntregadosHandler(RabbitTemplate template, String deadLetterExchange, String routingKeyNoEntregados) {
        this.template = template;
        this.deadLetterExchange = deadLetterExchange;
        this.routingKeyNoEntregados = routingKeyNoEntregados;
    }

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        Message mensaje = returned.getMessage();
        log.error("[NO ENTREGADO] exchange={} routingKey={} código={} motivo={} payload={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
                returned.getReplyText(), new String(mensaje.getBody(), StandardCharsets.UTF_8));

        if (mensaje.getMessageProperties().getHeaders().containsKey(HEADER_NO_ENTREGADO)) {
            log.error("[NO ENTREGADO] el reenvío a la cola de no entregados también falló; se descarta el mensaje");
            return;
        }
        mensaje.getMessageProperties().setHeader(HEADER_NO_ENTREGADO,
                returned.getExchange() + "/" + returned.getRoutingKey());
        executor.execute(() -> {
            try {
                template.send(deadLetterExchange, routingKeyNoEntregados, mensaje);
                log.warn("[NO ENTREGADO] mensaje reenviado a {} con routing key {}", deadLetterExchange, routingKeyNoEntregados);
            } catch (AmqpException e) {
                log.error("[NO ENTREGADO] no se pudo reenviar el mensaje: {}", e.getMessage());
            }
        });
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!ack) {
            log.error("[NO CONFIRMADO] RabbitMQ no confirmó la publicación (correlación={}): {}",
                    correlationData != null ? correlationData.getId() : "n/a", cause);
        }
    }
}
