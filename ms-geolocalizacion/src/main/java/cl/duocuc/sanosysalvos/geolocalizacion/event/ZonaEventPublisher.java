package cl.duocuc.sanosysalvos.geolocalizacion.event;

import cl.duocuc.sanosysalvos.geolocalizacion.config.MensajeriaProperties;
import cl.duocuc.sanosysalvos.geolocalizacion.model.ZonaReporte;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Productor de ms-geolocalizacion: publica {@code zona.actualizada} cuando se
 * registra una zona de reporte. El servicio de dominio no conoce RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZonaEventPublisher {

    private static final String SERVICIO_ORIGEN = "ms-geolocalizacion";

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public void publishZonaActualizada(ZonaReporte zona) {
        String routingKey = props.getRoutingKeys().getZonaActualizada();

        Map<String, Object> payload = new HashMap<>();
        payload.put("zonaId", zona.getId());
        payload.put("mascotaId", zona.getMascotaId());
        payload.put("usuarioId", zona.getUsuarioId());
        payload.put("latitud", zona.getLatitud());
        payload.put("longitud", zona.getLongitud());
        payload.put("tipoReporte", zona.getTipoReporte());
        payload.put("descripcion", zona.getDescripcion());
        payload.put("fechaReporte", zona.getFechaReporte() != null ? zona.getFechaReporte().toString() : null);

        Map<String, Object> evento = new HashMap<>();
        evento.put("evento", routingKey);
        evento.put("servicioOrigen", SERVICIO_ORIGEN);
        evento.put("payload", payload);

        rabbitTemplate.convertAndSend(props.getExchange(), routingKey, evento);
        log.info("Evento publicado: {} para zona ID {}", routingKey, zona.getId());
    }
}
