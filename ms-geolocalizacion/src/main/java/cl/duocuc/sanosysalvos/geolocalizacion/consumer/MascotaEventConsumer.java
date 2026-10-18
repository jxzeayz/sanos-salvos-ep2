package cl.duocuc.sanosysalvos.geolocalizacion.consumer;

import cl.duocuc.sanosysalvos.geolocalizacion.dto.ZonaReporteRequest;
import cl.duocuc.sanosysalvos.geolocalizacion.service.GeoService;
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
import java.util.Map;

/**
 * Consumidor del dominio <b>geolocalización</b>: recibe {@code mascota.perdida}
 * y {@code mascota.encontrada} y registra la zona del reporte.
 * <p>
 * Confirmación manual:
 * <ul>
 *   <li><b>ACK</b>: zona registrada, o el reporte no trae coordenadas (no aplica).</li>
 *   <li><b>NACK sin reencolar → DLQ</b>: evento inválido (JSON roto, sin mascotaId/usuarioId…).</li>
 *   <li><b>NACK reencolando (1 reintento)</b>: error recuperable; si falla de nuevo → DLQ.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MascotaEventConsumer {

    private static final TypeReference<Map<String, Object>> TIPO_EVENTO = new TypeReference<>() {
    };

    private final GeoService geoService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${mensajeria.cola.nombre}", ackMode = "MANUAL")
    public void procesarMascotaRegistrada(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        boolean reentregado = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            Map<String, Object> evento = leerEvento(message);
            ZonaReporteRequest request = aRequest(evento);

            if (request.getLatitud() == null || request.getLongitud() == null) {
                log.info("Reporte de mascota {} sin coordenadas: no se registra zona [{}]",
                        request.getMascotaId(), routingKey);
            } else {
                geoService.registrarZona(request);
                log.info("Zona de reporte creada desde evento para mascota ID {} [{}]",
                        request.getMascotaId(), routingKey);
            }
            channel.basicAck(tag, false);
        } catch (EventoInvalidoException e) {
            log.error("[DLQ] Evento inválido descartado de geolocalización [{}]: {} | payload={}",
                    routingKey, e.getMessage(), cuerpo(message));
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            if (reentregado) {
                log.error("[DLQ] Evento falló nuevamente tras reintento [{}]: {} | payload={}",
                        routingKey, e.getMessage(), cuerpo(message), e);
                channel.basicNack(tag, false, false);
            } else {
                log.warn("[REINTENTO] Error recuperable registrando zona [{}], se reencola: {}",
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

    private static ZonaReporteRequest aRequest(Map<String, Object> evento) {
        Long mascotaId = aLong(evento.get("mascotaId"), "mascotaId");
        Long usuarioId = aLong(evento.get("usuarioId"), "usuarioId");
        if (mascotaId == null || usuarioId == null) {
            throw new EventoInvalidoException("faltan los campos obligatorios 'mascotaId' y/o 'usuarioId'");
        }

        Object tipo = evento.get("tipoReporte") != null ? evento.get("tipoReporte") : evento.get("estado");
        if (tipo == null || tipo.toString().isBlank()) {
            throw new EventoInvalidoException("falta 'tipoReporte' o 'estado'");
        }

        ZonaReporteRequest request = new ZonaReporteRequest();
        request.setMascotaId(mascotaId);
        request.setUsuarioId(usuarioId);
        request.setLatitud(aDouble(evento.get("latitud"), "latitud"));
        request.setLongitud(aDouble(evento.get("longitud"), "longitud"));
        request.setTipoReporte(tipo.toString());
        request.setDescripcion(evento.get("descripcion") != null ? evento.get("descripcion").toString() : null);
        return request;
    }

    private static Long aLong(Object valor, String campo) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(valor.toString());
        } catch (NumberFormatException e) {
            throw new EventoInvalidoException("el campo '" + campo + "' no es numérico: " + valor);
        }
    }

    private static Double aDouble(Object valor, String campo) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.valueOf(valor.toString());
        } catch (NumberFormatException e) {
            throw new EventoInvalidoException("el campo '" + campo + "' no es numérico: " + valor);
        }
    }

    private static String cuerpo(Message message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }
}
