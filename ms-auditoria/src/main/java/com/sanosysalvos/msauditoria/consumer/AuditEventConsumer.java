package com.sanosysalvos.msauditoria.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sanosysalvos.msauditoria.model.AuditLog;
import com.sanosysalvos.msauditoria.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consumidor del dominio <b>auditoría</b>: registra cada evento publicado en
 * el exchange de eventos.
 * <p>
 * Confirmación manual:
 * <ul>
 *   <li><b>ACK</b>: evento registrado.</li>
 *   <li><b>NACK sin reencolar → DLQ</b>: el cuerpo no es un JSON válido.</li>
 *   <li><b>NACK reencolando (1 reintento)</b>: error recuperable; si falla de nuevo → DLQ.</li>
 * </ul>
 */
@Slf4j
@Component
public class AuditEventConsumer {

    private static final TypeReference<Map<String, Object>> TIPO_EVENTO = new TypeReference<>() {
    };
    private static final String DESCONOCIDO = "desconocido";

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${mensajeria.cola.nombre}", ackMode = "MANUAL")
    public void procesarEvento(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        boolean reentregado = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            AuditLog auditLog = aAuditLog(leerEvento(message), routingKey, message.getBody());

            auditService.guardar(auditLog);
            log.info("Evento de auditoría registrado: {} desde {}", auditLog.getEvento(), auditLog.getServicioOrigen());

            channel.basicAck(tag, false);
        } catch (EventoInvalidoException e) {
            log.error("[DLQ] Evento inválido descartado de auditoría [{}]: {} | payload={}",
                    routingKey, e.getMessage(), cuerpo(message.getBody()));
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            if (reentregado) {
                log.error("[DLQ] Evento de auditoría falló nuevamente tras reintento [{}]: {}",
                        routingKey, e.getMessage(), e);
                channel.basicNack(tag, false, false);
            } else {
                log.warn("[REINTENTO] Error recuperable registrando auditoría [{}], se reencola: {}",
                        routingKey, e.getMessage());
                channel.basicNack(tag, false, true);
            }
        }
    }

    private Map<String, Object> leerEvento(Message message) {
        try {
            return objectMapper.readValue(message.getBody(), TIPO_EVENTO);
        } catch (IOException e) {
            throw new EventoInvalidoException("el cuerpo no es un JSON válido", e);
        }
    }

    /** Si el evento no informa su nombre se usa la routing key con la que llegó. */
    private static AuditLog aAuditLog(Map<String, Object> evento, String routingKey, byte[] cuerpo) {
        Object nombre = evento.get("evento");
        Object origen = evento.get("servicioOrigen");

        AuditLog auditLog = new AuditLog();
        auditLog.setEvento(nombre != null ? nombre.toString() : (routingKey != null ? routingKey : DESCONOCIDO));
        auditLog.setServicioOrigen(origen != null ? origen.toString() : DESCONOCIDO);
        auditLog.setPayload(cuerpo(cuerpo));
        auditLog.setFechaEvento(LocalDateTime.now());
        return auditLog;
    }

    private static String cuerpo(byte[] cuerpo) {
        return new String(cuerpo, StandardCharsets.UTF_8);
    }
}
