package com.sanosysalvos.msauditoria.consumer;

/**
 * Error NO recuperable: el mensaje está mal formado o le faltan datos.
 * Se rechaza sin reencolar y termina en la DLQ.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public EventoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
