package cl.duocuc.sanosysalvos.matching.consumer;

import cl.duocuc.sanosysalvos.matching.config.MensajeriaProperties;
import cl.duocuc.sanosysalvos.matching.model.MascotaSnapshot;
import cl.duocuc.sanosysalvos.matching.service.MatchingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Consumidor del dominio <b>matching</b>: recibe los eventos {@code mascota.*}
 * y ejecuta el motor de coincidencias.
 * <p>
 * Confirmación manual (ackMode = MANUAL):
 * <ul>
 *   <li><b>ACK</b>: el evento se procesó correctamente (o no aplica, como mascota.eliminada).</li>
 *   <li><b>NACK sin reencolar → DLQ</b>: el evento es inválido (error no recuperable).</li>
 *   <li><b>NACK reencolando (1 reintento)</b>: error recuperable (p. ej. la BD no responde).
 *       Si el mensaje ya era una re-entrega y vuelve a fallar, va a la DLQ.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MascotaEventConsumer {

    private static final TypeReference<Map<String, Object>> TIPO_EVENTO = new TypeReference<>() {
    };

    private final MatchingService matchingService;
    private final ObjectMapper objectMapper;
    private final MensajeriaProperties props;

    @RabbitListener(queues = "${mensajeria.cola.nombre}", ackMode = "MANUAL")
    public void onMascotaEvent(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        boolean reentregado = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            if (props.getRoutingKeys().getMascotaEliminada().equals(routingKey)) {
                log.info("Evento {} no aplica al motor de matching; se confirma sin procesar", routingKey);
                channel.basicAck(tag, false);
                return;
            }
            Map<String, Object> evento = leerEvento(message);
            log.info("Evento recibido para matching [{}]: mascotaId={}", routingKey, evento.get("mascotaId"));

            matchingService.procesarNuevaMascota(aSnapshot(evento));

            channel.basicAck(tag, false);
        } catch (EventoInvalidoException e) {
            log.error("[DLQ] Evento inválido descartado de matching [{}]: {} | payload={}",
                    routingKey, e.getMessage(), cuerpo(message));
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            if (reentregado) {
                log.error("[DLQ] Evento falló nuevamente tras reintento [{}]: {} | payload={}",
                        routingKey, e.getMessage(), cuerpo(message), e);
                channel.basicNack(tag, false, false);
            } else {
                log.warn("[REINTENTO] Error recuperable procesando evento [{}], se reencola: {}",
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

    private MascotaSnapshot aSnapshot(Map<String, Object> evento) {
        return MascotaSnapshot.builder()
                .mascotaId(requerirLong(evento, "mascotaId"))
                .usuarioId(requerirLong(evento, "usuarioId"))
                .nombre((String) evento.get("nombre"))
                .especie(requerirTexto(evento, "especie"))
                .raza((String) evento.get("raza"))
                .color((String) evento.get("color"))
                .tamano((String) evento.get("tamano"))
                .descripcion((String) evento.get("descripcion"))
                .estado(requerirTexto(evento, "estado"))
                .latitud(aDouble(evento.get("latitud"), "latitud"))
                .longitud(aDouble(evento.get("longitud"), "longitud"))
                .fechaReporte(aFecha(evento.get("fechaReporte")))
                .build();
    }

    private static Long requerirLong(Map<String, Object> evento, String campo) {
        Object valor = evento.get(campo);
        if (valor == null) {
            throw new EventoInvalidoException("falta el campo obligatorio '" + campo + "'");
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

    private static String requerirTexto(Map<String, Object> evento, String campo) {
        Object valor = evento.get(campo);
        if (valor == null || valor.toString().isBlank()) {
            throw new EventoInvalidoException("falta el campo obligatorio '" + campo + "'");
        }
        return valor.toString();
    }

    private static Double aDouble(Object valor, String campo) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(valor.toString());
        } catch (NumberFormatException e) {
            throw new EventoInvalidoException("el campo '" + campo + "' no es numérico: " + valor);
        }
    }

    private static LocalDateTime aFecha(Object valor) {
        if (valor == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(valor.toString());
        } catch (DateTimeParseException e) {
            throw new EventoInvalidoException("fechaReporte con formato inválido: " + valor);
        }
    }

    private static String cuerpo(Message message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }
}
