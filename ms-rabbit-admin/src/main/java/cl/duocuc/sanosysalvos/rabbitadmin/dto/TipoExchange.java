package cl.duocuc.sanosysalvos.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.amqp.core.ExchangeTypes;

import java.util.Arrays;

/** Tipos de exchange soportados por RabbitMQ. Acepta mayúsculas o minúsculas. */
public enum TipoExchange {

    DIRECT(ExchangeTypes.DIRECT),
    TOPIC(ExchangeTypes.TOPIC),
    FANOUT(ExchangeTypes.FANOUT),
    HEADERS(ExchangeTypes.HEADERS);

    private final String valor;

    TipoExchange(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoExchange desde(String texto) {
        if (texto == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(t -> t.valor.equalsIgnoreCase(texto.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "tipo de exchange inválido: '" + texto + "'. Valores permitidos: direct, topic, fanout, headers"));
    }
}
