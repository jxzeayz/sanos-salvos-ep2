package cl.duocuc.sanosysalvos.matching.consumer;

import cl.duocuc.sanosysalvos.matching.config.MensajeriaProperties;
import cl.duocuc.sanosysalvos.matching.model.MascotaSnapshot;
import cl.duocuc.sanosysalvos.matching.service.MatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifica las decisiones de ACK / NACK / DLQ del consumidor de matching. */
@ExtendWith(MockitoExtension.class)
class MascotaEventConsumerTest {

    private static final long TAG = 7L;
    private static final String EVENTO_VALIDO = """
            {"mascotaId":10,"usuarioId":3,"nombre":"Toby","especie":"PERRO","estado":"PERDIDA",
             "latitud":-33.45,"longitud":-70.66,"fechaReporte":"2026-10-01T10:15:30"}
            """;

    @Mock private MatchingService matchingService;
    @Mock private Channel channel;

    private MascotaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MensajeriaProperties props = new MensajeriaProperties();
        props.getRoutingKeys().setMascotaEliminada("mascota.eliminada");
        consumer = new MascotaEventConsumer(matchingService, new ObjectMapper(), props);
    }

    private static Message mensaje(String json, boolean reentregado) {
        return mensaje(json, reentregado, "mascota.perdida");
    }

    private static Message mensaje(String json, boolean reentregado, String routingKey) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setRedelivered(reentregado);
        props.setReceivedRoutingKey(routingKey);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void eventoValido_seProcesaYConfirmaConAck() throws Exception {
        consumer.onMascotaEvent(mensaje(EVENTO_VALIDO, false), channel);

        ArgumentCaptor<MascotaSnapshot> captor = ArgumentCaptor.forClass(MascotaSnapshot.class);
        verify(matchingService).procesarNuevaMascota(captor.capture());
        assertThat(captor.getValue().getMascotaId()).isEqualTo(10L);
        assertThat(captor.getValue().getEstado()).isEqualTo("PERDIDA");
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void mascotaEliminada_seConfirmaSinEjecutarMatching() throws Exception {
        consumer.onMascotaEvent(mensaje(EVENTO_VALIDO, false, "mascota.eliminada"), channel);

        verify(matchingService, never()).procesarNuevaMascota(any());
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void jsonMalFormado_vaALaDlqSinReencolar() throws Exception {
        consumer.onMascotaEvent(mensaje("esto no es json", false), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(matchingService, never()).procesarNuevaMascota(any());
    }

    @Test
    void eventoSinMascotaId_vaALaDlqSinReencolar() throws Exception {
        consumer.onMascotaEvent(mensaje("{\"especie\":\"PERRO\",\"estado\":\"PERDIDA\",\"usuarioId\":1}", false), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void errorRecuperableEnPrimeraEntrega_seReencola() throws Exception {
        when(matchingService.procesarNuevaMascota(any()))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        consumer.onMascotaEvent(mensaje(EVENTO_VALIDO, false), channel);

        verify(channel).basicNack(TAG, false, true);
    }

    @Test
    void errorRecuperableEnReentrega_vaALaDlq() throws Exception {
        when(matchingService.procesarNuevaMascota(any()))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        consumer.onMascotaEvent(mensaje(EVENTO_VALIDO, true), channel);

        verify(channel).basicNack(TAG, false, false);
    }
}
