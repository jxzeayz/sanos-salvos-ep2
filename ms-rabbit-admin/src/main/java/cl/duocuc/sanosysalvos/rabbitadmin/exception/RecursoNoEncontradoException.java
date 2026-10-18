package cl.duocuc.sanosysalvos.rabbitadmin.exception;

/** HTTP 404 — la cola, exchange o binding no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
