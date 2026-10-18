package cl.duocuc.sanosysalvos.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos para crear una cola. Los campos opcionales se traducen a argumentos
 * de RabbitMQ (x-message-ttl, x-max-length, x-dead-letter-*).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearColaRequest {

    @NotBlank(message = "el nombre de la cola es obligatorio")
    @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = "nombre " + NombresRabbit.MENSAJE_NOMBRE)
    private String nombre;

    /** Sobrevive a reinicios del broker. Por defecto true. */
    private Boolean durable;

    /** Exclusiva de la conexión que la crea. Por defecto false. */
    private Boolean exclusiva;

    /** Se elimina cuando deja de tener consumidores. Por defecto false. */
    private Boolean autoEliminar;

    @Positive(message = "ttlMensajesMs debe ser mayor que 0")
    @Max(value = 604_800_000L, message = "ttlMensajesMs no puede superar 7 días (604800000 ms)")
    private Long ttlMensajesMs;

    @Positive(message = "maxMensajes debe ser mayor que 0")
    @Max(value = 1_000_000L, message = "maxMensajes no puede superar 1.000.000")
    private Integer maxMensajes;

    @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = "deadLetterExchange " + NombresRabbit.MENSAJE_NOMBRE)
    private String deadLetterExchange;

    @Pattern(regexp = NombresRabbit.PATRON_ROUTING_KEY, message = "deadLetterRoutingKey " + NombresRabbit.MENSAJE_ROUTING_KEY)
    private String deadLetterRoutingKey;

    /** Una routing key de dead-letter sin exchange de dead-letter no tiene efecto. */
    @AssertTrue(message = "deadLetterRoutingKey requiere indicar deadLetterExchange")
    public boolean isDeadLetterConsistente() {
        return deadLetterRoutingKey == null || deadLetterExchange != null;
    }

    /** Una cola exclusiva siempre es temporal: no puede ser durable. */
    @AssertTrue(message = "una cola exclusiva no puede ser durable")
    public boolean isExclusivaNoDurable() {
        return !Boolean.TRUE.equals(exclusiva) || Boolean.FALSE.equals(durable);
    }
}
