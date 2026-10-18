package cl.duocuc.sanosysalvos.rabbitadmin.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Modelos de respuesta de la API del administrador. */
public final class Respuestas {

    private Respuestas() {
    }

    public record ColaResponse(String nombre, Long mensajes, Long consumidores,
                               Boolean durable, Map<String, Object> argumentos) {
    }

    public record ExchangeResponse(String nombre, String tipo, Boolean durable, Boolean autoEliminar) {
    }

    public record BindingResponse(String exchange, String cola, String routingKey) {
    }

    public record PurgaResponse(String cola, int mensajesEliminados) {
    }

    public record PublicacionResponse(String exchange, String routingKey, String estado) {
    }

    /** Mensaje leído de una cola sin consumirlo (se reencola). */
    public record MensajeResponse(String routingKey, String exchange, Boolean reentregado,
                                  Map<String, Object> propiedades, Object payload) {
    }

    public record ErrorResponse(LocalDateTime timestamp, int status, String error,
                                String mensaje, List<String> detalles) {
    }
}
