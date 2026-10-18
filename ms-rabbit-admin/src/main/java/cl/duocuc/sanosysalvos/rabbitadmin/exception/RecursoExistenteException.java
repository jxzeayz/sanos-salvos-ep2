package cl.duocuc.sanosysalvos.rabbitadmin.exception;

/** HTTP 409 — ya existe un recurso con ese nombre. */
public class RecursoExistenteException extends RuntimeException {

    public RecursoExistenteException(String mensaje) {
        super(mensaje);
    }

    public RecursoExistenteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
