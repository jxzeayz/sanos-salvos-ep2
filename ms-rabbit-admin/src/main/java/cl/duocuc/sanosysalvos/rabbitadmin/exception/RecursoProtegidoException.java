package cl.duocuc.sanosysalvos.rabbitadmin.exception;

/** HTTP 403 — recurso del sistema que no se puede modificar desde la API. */
public class RecursoProtegidoException extends RuntimeException {

    public RecursoProtegidoException(String mensaje) {
        super(mensaje);
    }

    public RecursoProtegidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
