package cl.duocuc.sanosysalvos.matching.event;

import cl.duocuc.sanosysalvos.matching.config.MensajeriaProperties;
import cl.duocuc.sanosysalvos.matching.model.Coincidencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Productor de ms-matching: publica {@code coincidencia.hallada} en el exchange
 * de eventos cuando el motor detecta una coincidencia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoincidenciaEventPublisher {

    private static final String SERVICIO_ORIGEN = "ms-matching";

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public void publishCoincidenciaHallada(Coincidencia coincidencia) {
        String routingKey = props.getRoutingKeys().getCoincidenciaHallada();

        Map<String, Object> evento = new HashMap<>();
        evento.put("evento", routingKey);
        evento.put("servicioOrigen", SERVICIO_ORIGEN);
        evento.put("coincidenciaId", coincidencia.getId());
        evento.put("mascotaPerdidaId", coincidencia.getMascotaPerdidaId());
        evento.put("mascotaEncontradaId", coincidencia.getMascotaEncontradaId());
        evento.put("usuarioId", coincidencia.getUsuarioIdPerdida());
        evento.put("usuarioIdEncontrada", coincidencia.getUsuarioIdEncontrada());
        evento.put("scoreMatch", coincidencia.getScoreMatch());
        evento.put("estado", coincidencia.getEstado().name());
        evento.put("fechaDeteccion", coincidencia.getFechaDeteccion() != null
                ? coincidencia.getFechaDeteccion().toString() : null);
        evento.put("mensaje", "Se encontró una mascota que coincide con tu reporte de mascota perdida");

        rabbitTemplate.convertAndSend(props.getExchange(), routingKey, evento);
        log.info("Evento publicado: {} para coincidencia ID {}", routingKey, coincidencia.getId());
    }
}
