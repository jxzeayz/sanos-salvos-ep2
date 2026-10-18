package cl.duocuc.sanosysalvos.rabbitadmin.dto;

/**
 * Reglas de nombres compartidas por los DTO y el servicio.
 * RabbitMQ reserva el prefijo "amq." y limita los nombres a 255 caracteres.
 */
public final class NombresRabbit {

    /** Nombre de cola o exchange: sin espacios, sin prefijo reservado "amq.". */
    public static final String PATRON_NOMBRE = "^(?!amq\\.)[A-Za-z0-9._:-]{1,255}$";

    public static final String MENSAJE_NOMBRE =
            "solo admite letras, números, '.', '_', '-' y ':' (máx. 255) y no puede comenzar con 'amq.'";

    /** Routing key: puede ser vacía (fanout) y admite comodines '*' y '#'. */
    public static final String PATRON_ROUTING_KEY = "^[A-Za-z0-9._:*#-]{0,255}$";

    public static final String MENSAJE_ROUTING_KEY =
            "solo admite letras, números, '.', '_', '-', ':', '*' y '#' (máx. 255)";

    private NombresRabbit() {
    }
}
