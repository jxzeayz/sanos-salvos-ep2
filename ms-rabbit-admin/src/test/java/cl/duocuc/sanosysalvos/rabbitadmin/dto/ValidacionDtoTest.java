package cl.duocuc.sanosysalvos.rabbitadmin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifica que la API no acepte colas/exchanges/bindings con datos inválidos. */
class ValidacionDtoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static <T> Set<ConstraintViolation<T>> validar(T dto) {
        return validator.validate(dto);
    }

    @Test
    void cola_valida_sinErrores() {
        assertThat(validar(CrearColaRequest.builder().nombre("q.pruebas-1").build())).isEmpty();
    }

    @Test
    void cola_nombreVacio_esInvalida() {
        assertThat(validar(CrearColaRequest.builder().nombre("  ").build())).isNotEmpty();
        assertThat(validar(CrearColaRequest.builder().build())).isNotEmpty();
    }

    @Test
    void cola_nombreConEspaciosOReservado_esInvalida() {
        assertThat(validar(CrearColaRequest.builder().nombre("mi cola").build())).isNotEmpty();
        assertThat(validar(CrearColaRequest.builder().nombre("amq.mia").build())).isNotEmpty();
    }

    @Test
    void cola_ttlYMaxNegativos_sonInvalidos() {
        CrearColaRequest req = CrearColaRequest.builder().nombre("q.ok").ttlMensajesMs(-1L).maxMensajes(0).build();
        assertThat(validar(req)).hasSize(2);
    }

    @Test
    void cola_routingKeyDeadLetterSinExchange_esInvalida() {
        CrearColaRequest req = CrearColaRequest.builder().nombre("q.ok").deadLetterRoutingKey("q.ok.dlq").build();
        assertThat(validar(req))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("deadLetterRoutingKey requiere indicar deadLetterExchange");
    }

    @Test
    void cola_exclusivaDurable_esInvalida() {
        CrearColaRequest req = CrearColaRequest.builder().nombre("q.ok").exclusiva(true).build();
        assertThat(validar(req)).hasSize(1);
        req.setDurable(false);
        assertThat(validar(req)).isEmpty();
    }

    @Test
    void exchange_sinTipo_esInvalido() {
        assertThat(validar(CrearExchangeRequest.builder().nombre("pruebas.events").build())).hasSize(1);
    }

    @Test
    void tipoExchange_aceptaMayusculasYRechazaDesconocidos() {
        assertThat(TipoExchange.desde("Topic")).isEqualTo(TipoExchange.TOPIC);
        assertThatThrownBy(() -> TipoExchange.desde("x-delayed")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void binding_conComodines_esValido_yVaciaPermitida() {
        assertThat(validar(new BindingRequest("sanos-salvos.events", "q.ok", "mascota.#"))).isEmpty();
        assertThat(validar(new BindingRequest("pruebas.fanout", "q.ok", ""))).isEmpty();
    }

    @Test
    void binding_sinColaNiRoutingKey_esInvalido() {
        assertThat(validar(new BindingRequest("sanos-salvos.events", "", null))).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void publicacion_conComodinEnRoutingKey_esInvalida() {
        assertThat(validar(new PublicarMensajeRequest("sanos-salvos.events", "mascota.*", Map.of("a", 1)))).hasSize(1);
        assertThat(validar(new PublicarMensajeRequest("sanos-salvos.events", "mascota.perdida", Map.of()))).hasSize(1);
    }
}
