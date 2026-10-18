package com.sanosysalvos.msnotificaciones.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sanosysalvos.msnotificaciones.model.Notificacion;
import com.sanosysalvos.msnotificaciones.model.TipoNotificacion;
import com.sanosysalvos.msnotificaciones.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consumidor del dominio <b>notificaciones</b>: recibe {@code coincidencia.*}
 * y crea la notificación para el dueño de la mascota perdida.
 * <p>
 * Confirmación manual:
 * <ul>
 *   <li><b>ACK</b>: notificación guardada.</li>
 *   <li><b>NACK sin reencolar → DLQ</b>: evento inválido (sin usuarioId, JSON roto…).</li>
 *   <li><b>NACK reencolando (1 reintento)</b>: error recuperable; si falla de nuevo → DLQ.</li>
 * </ul>
 */
@Slf4j
@Component
public class CoincidenciaEventConsumer {

    private static final TypeReference<Map<String, Object>> TIPO_EVENTO = new TypeReference<>() {
    };

    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    public CoincidenciaEventConsumer(NotificacionService notificacionService, ObjectMapper objectMapper) {
        this.notificacionService = notificacionService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${mensajeria.cola.nombre}", ackMode = "MANUAL")
    public void procesarCoincidencia(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        boolean reentregado = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            Map<String, Object> evento = leerEvento(message);
            Notificacion notificacion = aNotificacion(evento);

            notificacionService.guardar(notificacion);
            log.info("Notificación guardada para usuario {} [{}]", notificacion.getUsuarioId(), routingKey);

            channel.basicAck(tag, false);
        } catch (EventoInvalidoException e) {
            log.error("[DLQ] Evento inválido descartado de notificaciones [{}]: {} | payload={}",
                    routingKey, e.getMessage(), cuerpo(message));
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            if (reentregado) {
                log.error("[DLQ] Evento falló nuevamente tras reintento [{}]: {} | payload={}",
                        routingKey, e.getMessage(), cuerpo(message), e);
                channel.basicNack(tag, false, false);
            } else {
                log.warn("[REINTENTO] Error recuperable guardando notificación [{}], se reencola: {}",
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

    private Notificacion aNotificacion(Map<String, Object> evento) {
        Long usuarioId = aLong(evento.get("usuarioId"), "usuarioId");
        if (usuarioId == null) {
            throw new EventoInvalidoException("falta el campo obligatorio 'usuarioId'");
        }

        String mensaje = evento.get("mensaje") != null
                ? evento.get("mensaje").toString()
                : "Se ha encontrado una mascota que coincide con tu reporte";
        if (evento.get("scoreMatch") instanceof Number score) {
            mensaje += " (compatibilidad: " + String.format("%.0f", score.doubleValue() * 100) + "%)";
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setTipo(TipoNotificacion.Tipo.COINCIDENCIA);
        notificacion.setTitulo("Coincidencia detectada");
        notificacion.setMensaje(mensaje);
        notificacion.setUsuarioId(usuarioId);
        notificacion.setOrigenUsuarioId(aLong(evento.get("usuarioIdEncontrada"), "usuarioIdEncontrada"));
        notificacion.setEnviadaEn(LocalDateTime.now());
        return notificacion;
    }

    private static Long aLong(Object valor, String campo) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(valor.toString());
        } catch (NumberFormatException e) {
            throw new EventoInvalidoException("el campo '" + campo + "' no es numérico: " + valor);
        }
    }

    private static String cuerpo(Message message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }
}
