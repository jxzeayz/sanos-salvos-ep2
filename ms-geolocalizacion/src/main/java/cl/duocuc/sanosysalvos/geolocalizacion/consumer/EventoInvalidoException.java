package cl.duocuc.sanosysalvos.geolocalizacion.consumer;

/**
 * Error NO recuperable: el mensaje está mal formado o le faltan datos.
 * Reintentarlo no sirve, por lo que se rechaza sin reencolar y va a la DLQ.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public EventoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
