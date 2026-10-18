package cl.duocuc.sanosysalvos.mascotas.event;

import cl.duocuc.sanosysalvos.mascotas.config.MensajeriaProperties;
import cl.duocuc.sanosysalvos.mascotas.model.Mascota;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Productor de ms-mascotas: publica los eventos de ciclo de vida de una
 * mascota. La routing key depende del estado ({prefijo}.perdida,
 * {prefijo}.encontrada, {prefijo}.reunificada) o de la eliminación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MascotaEventPublisher {

    private static final String SERVICIO_ORIGEN = "ms-mascotas";

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public void publishMascotaRegistrada(Mascota mascota) {
        publicar(mascota, "mascota.registrada", routingKeyPorEstado(mascota, "registrada"));
    }

    public void publishMascotaActualizada(Mascota mascota) {
        publicar(mascota, "mascota.actualizada", routingKeyPorEstado(mascota, "actualizada"));
    }

    public void publishMascotaEstadoActualizado(Mascota mascota) {
        publicar(mascota, "mascota.estado_actualizado", routingKeyPorEstado(mascota, "estado_actualizado"));
    }

    public void publishMascotaEliminada(Mascota mascota) {
        publicar(mascota, "mascota.eliminada", props.getRoutingKeys().getMascotaEliminada());
    }

    private void publicar(Mascota mascota, String nombreEvento, String routingKey) {
        Map<String, Object> evento = buildEventoBase(mascota);
        evento.put("evento", nombreEvento);
        rabbitTemplate.convertAndSend(props.getExchange(), routingKey, evento);
        log.info("Evento publicado: {} (routing key {}) para mascota ID {}", nombreEvento, routingKey, mascota.getId());
    }

    private String routingKeyPorEstado(Mascota mascota, String sinEstado) {
        String sufijo = mascota.getEstado() != null ? mascota.getEstado().name().toLowerCase() : sinEstado;
        return props.getRoutingKeys().getPrefijoMascota() + "." + sufijo;
    }

    private Map<String, Object> buildEventoBase(Mascota mascota) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("servicioOrigen", SERVICIO_ORIGEN);
        evento.put("mascotaId", mascota.getId());
        evento.put("usuarioId", mascota.getUsuarioId());
        evento.put("nombre", mascota.getNombre());
        evento.put("especie", mascota.getEspecie() != null ? mascota.getEspecie().name() : null);
        evento.put("raza", mascota.getRaza());
        evento.put("color", mascota.getColor());
        evento.put("tamano", mascota.getTamano() != null ? mascota.getTamano().name() : null);
        evento.put("estado", mascota.getEstado() != null ? mascota.getEstado().name() : null);
        evento.put("tipoReporte", mascota.getEstado() != null ? mascota.getEstado().name() : null);
        evento.put("descripcion", mascota.getDescripcion());
        evento.put("latitud", mascota.getLatitud());
        evento.put("longitud", mascota.getLongitud());
        evento.put("fechaReporte", mascota.getFechaReporte() != null ? mascota.getFechaReporte().toString() : null);
        return evento;
    }
}
