package cl.duocuc.sanosysalvos.rabbitadmin.exception;

/** HTTP 503 — no se pudo comunicar con RabbitMQ. */
public class BrokerNoDisponibleException extends RuntimeException {

    public BrokerNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public BrokerNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
