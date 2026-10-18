package com.sanosysalvos.msnotificaciones.test.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sanosysalvos.msnotificaciones.consumer.CoincidenciaEventConsumer;
import com.sanosysalvos.msnotificaciones.model.Notificacion;
import com.sanosysalvos.msnotificaciones.service.NotificacionService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Verifica las decisiones de ACK / NACK / DLQ del consumidor de notificaciones. */
@ExtendWith(MockitoExtension.class)
class CoincidenciaEventConsumerTest {

    private static final long TAG = 3L;
    private static final String EVENTO_VALIDO =
            "{\"usuarioId\":5,\"usuarioIdEncontrada\":9,\"scoreMatch\":0.82,\"mensaje\":\"Coincidencia\"}";

    @Mock private NotificacionService notificacionService;
    @Mock private Channel channel;

    private CoincidenciaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CoincidenciaEventConsumer(notificacionService, new ObjectMapper());
    }

    private static Message mensaje(String json, boolean reentregado) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setRedelivered(reentregado);
        props.setReceivedRoutingKey("coincidencia.hallada");
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void eventoValido_guardaNotificacionYHaceAck() throws Exception {
        consumer.procesarCoincidencia(mensaje(EVENTO_VALIDO, false), channel);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionService).guardar(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(5L);
        assertThat(captor.getValue().getOrigenUsuarioId()).isEqualTo(9L);
        assertThat(captor.getValue().getMensaje()).contains("82%");
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void eventoSinUsuarioId_vaALaDlq() throws Exception {
        consumer.procesarCoincidencia(mensaje("{\"scoreMatch\":0.9}", false), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(notificacionService, never()).guardar(any());
    }

    @Test
    void jsonMalFormado_vaALaDlq() throws Exception {
        consumer.procesarCoincidencia(mensaje("{roto", false), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void errorDeBaseDeDatos_primeraVezSeReencola_segundaVaALaDlq() throws Exception {
        doThrow(new DataAccessResourceFailureException("BD caída")).when(notificacionService).guardar(any());

        consumer.procesarCoincidencia(mensaje(EVENTO_VALIDO, false), channel);
        verify(channel).basicNack(TAG, false, true);

        consumer.procesarCoincidencia(mensaje(EVENTO_VALIDO, true), channel);
        verify(channel).basicNack(TAG, false, false);
    }
}
